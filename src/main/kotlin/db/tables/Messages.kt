package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.Broker
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.messagesBroker
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** Messages checked to exist can optionally be filtered by this [Op]. */
private typealias Filter = Op<Boolean>?

/** @see [MessageStatuses] */
object Messages : IntIdTable() {
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val sent: Column<LocalDateTime> = datetime("sent").clientDefault { LocalDateTime.now() }
    private val senderId: Column<Int> = integer("sender_id").references(Users.id)

    /** Text messages cannot exceed this length. */
    const val MAX_TEXT_LENGTH = 10_000

    private val text: Column<String> = varchar("text", MAX_TEXT_LENGTH)

    private data class ChatAndMessageId(val chatId: Int, val messageId: Int)

    private enum class CursorType {
        /** First message's cursor. */
        START,

        /** Last message's cursor. */
        END
    }

    /** Whether messages exist before or after a particular point in time. */
    private enum class Chronology { BEFORE, AFTER }

    /**
     * Clients who have [Broker.subscribe]d to [MessagesSubscription]s via [messagesBroker] will be notified.
     *
     * @throws [IllegalArgumentException] if the [userId] isn't in the [chatId].
     */
    fun create(chatId: Int, userId: Int, text: TextMessage) {
        if (!isUserInChat(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't in the chat (ID: $chatId).")
        val row = transaction {
            insert {
                it[this.chatId] = chatId
                it[Messages.text] = text.value
                it[senderId] = userId
            }.resultedValues!![0]
        }
        messagesBroker.notify(buildMessage(row).toNewMessage()) { isUserInChat(it.userId, chatId) }
    }

    /** Case-insensitively [query]s the [chatId]'s text messages. */
    fun searchGroupChat(chatId: Int, query: String, pagination: BackwardPagination? = null): List<MessageEdge> =
        search(readGroupChat(chatId, pagination), query)

    /**
     * [query]s the private chat [id]'s [Message]s which haven't been deleted (such as through [PrivateChatDeletions])
     * by the [userId].
     */
    fun searchPrivateChat(
        chatId: Int,
        userId: Int,
        query: String,
        pagination: BackwardPagination? = null
    ): List<MessageEdge> = search(readPrivateChat(chatId, userId, pagination), query)

    /**
     * Case-insensitively [query]s [MessageEdge.node]s.
     *
     * @see [searchGroupChat]
     * @see [searchPrivateChat]
     */
    private fun search(edges: List<MessageEdge>, query: String): List<MessageEdge> =
        edges.filter { it.node.text.value.contains(query, ignoreCase = true) }

    /**
     * The private chat [id]'s [Message]s which haven't been deleted (such as through [PrivateChatDeletions]) by the
     * [userId].
     *
     * @see [readPrivateChatConnection]
     */
    fun readPrivateChat(id: Int, userId: Int, pagination: BackwardPagination? = null): List<MessageEdge> {
        val op = PrivateChatDeletions.readLastDeletion(id, userId)?.let { sent greater it }
        return readChat(id, pagination, op)
    }

    /** @see [readGroupChatConnection] */
    fun readGroupChat(id: Int, pagination: BackwardPagination? = null): List<MessageEdge> = readChat(id, pagination)

    /**
     * The [MessageEdge]s in the chat [id].
     *
     * @see [readPrivateChat]
     * @see [readGroupChat]
     */
    private fun readChat(id: Int, pagination: BackwardPagination? = null, filter: Filter = null): List<MessageEdge> {
        val (last, before) = pagination ?: BackwardPagination()
        var op = chatId eq id
        before?.let { op = op and (Messages.id less it) }
        filter?.let { op = op and it }
        return transaction {
            select(op)
                .orderBy(Messages.id, SortOrder.DESC)
                .let { if (last == null) it else it.limit(last) }
                .reversed()
                .map { MessageEdge(buildMessage(it), cursor = it[Messages.id].value) }
        }
    }

    /** The message IDs in the [chatId] in order of creation. */
    fun readIdList(chatId: Int): List<Int> = transaction {
        select { Messages.chatId eq chatId }.map { it[Messages.id].value }
    }

    fun read(id: Int): Message = transaction {
        select { Messages.id eq id }.first().let(::buildMessage)
    }

    /**
     * The ID of the chat which contains the [messageId].
     *
     * @see [Messages.exists]
     */
    fun readChatFromMessage(messageId: Int): Int = transaction {
        select { Messages.id eq messageId }.first()[chatId]
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] in the [chatId]. Clients will be notified of a
     * [DeletionOfEveryMessage] via [messagesBroker].
     */
    fun deleteChat(chatId: Int) {
        val messageIdList = readMessageIdList(chatId)
        MessageStatuses.delete(messageIdList)
        transaction {
            deleteWhere { Messages.chatId eq chatId }
        }
        messagesBroker.notify(DeletionOfEveryMessage(chatId)) { isUserInChat(it.userId, chatId) }
    }

    /** Deletes all [Messages] and [MessageStatuses] in the [chatId] [until] the specified [LocalDateTime]. */
    fun deleteChatMessagesUntil(chatId: Int, until: LocalDateTime) {
        val idList = readMessageIdList(chatId, sent less until)
        MessageStatuses.delete(idList)
        transaction {
            deleteWhere { (Messages.chatId eq chatId) and (sent less until) }
        }
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] the [userId] created in the [chatId]. Clients who have
     * [Broker.subscribe]d to [MessagesSubscription]s via [messagesBroker] will be be notified of the
     * [UserChatMessagesRemoval].
     */
    fun deleteUserChatMessages(chatId: Int, userId: Int) {
        MessageStatuses.deleteUserChatStatuses(chatId, userId)
        val idList = readMessageIdList(chatId, senderId eq userId)
        MessageStatuses.delete(idList)
        transaction {
            deleteWhere { Messages.id inList idList }
        }
        messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] the [userId] has. Clients who have [Broker.subscribe]d to
     * [MessagesSubscription]s via [messagesBroker] will be notified of the [UserChatMessagesRemoval].
     */
    fun deleteUserMessages(userId: Int) {
        MessageStatuses.deleteUserStatuses(userId)
        val chatMessages = readChatMessages(userId)
        MessageStatuses.delete(chatMessages.map { it.messageId })
        transaction {
            deleteWhere { senderId eq userId }
        }
        chatMessages.forEach { (chatId) ->
            messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
        }
    }

    /**
     * Deletes the message [id] in the [chatId] from [Messages] and [MessageStatuses]. Clients who have
     * [Broker.subscribe]d to [MessagesSubscription] via [messagesBroker] will be notified of the
     * [DeletedMessage].
     */
    fun delete(id: Int) {
        MessageStatuses.delete(id)
        val chatId = readChatFromMessage(id)
        transaction {
            deleteWhere { Messages.id eq id }
        }
        messagesBroker.notify(DeletedMessage(chatId, id)) { isUserInChat(it.userId, chatId) }
    }

    /** Every [ChatAndMessageId] the [userId] created which are visible to at least one user. */
    private fun readChatMessages(userId: Int): List<ChatAndMessageId> = transaction {
        select { senderId eq userId }.map { ChatAndMessageId(it[chatId], it[Messages.id].value) }
    }

    /** Whether there are messages in the [chatId] [from] the [LocalDateTime]. */
    fun existsFrom(chatId: Int, from: LocalDateTime): Boolean = transaction {
        !select { (Messages.chatId eq chatId) and (sent greaterEq from) }.empty()
    }

    /** The [id] list for the [chatId]. */
    private fun readMessageIdList(chatId: Int, filter: Filter = null): List<Int> = transaction {
        val chatOp = Messages.chatId eq chatId
        val op = if (filter == null) chatOp else chatOp and filter
        select(op).map { it[Messages.id].value }
    }

    /** Whether the [messageId] exists in the [chatId]. */
    fun existsInChat(messageId: Int, chatId: Int): Boolean = transaction {
        !select { (Messages.chatId eq chatId) and (Messages.id eq messageId) }.empty()
    }

    fun exists(id: Int): Boolean = transaction {
        !select { Messages.id eq id }.empty()
    }

    private fun buildMessage(row: ResultRow): Message {
        val id = row[id].value
        val dateTimes = MessageDateTimes(row[sent], MessageStatuses.read(id))
        val sender = readUserById(row[senderId])
        return Message(id, sender, TextMessage(row[text]), dateTimes)
    }

    fun readGroupChatConnection(chatId: Int, pagination: BackwardPagination? = null): MessagesConnection =
        MessagesConnection(readGroupChat(chatId, pagination), buildPageInfo(chatId, pagination?.before))

    fun readPrivateChatConnection(
        chatId: Int,
        userId: Int,
        pagination: BackwardPagination? = null
    ): MessagesConnection {
        val op = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        return MessagesConnection(
            readPrivateChat(chatId, userId, pagination),
            buildPageInfo(chatId, pagination?.before, op)
        )
    }

    /**
     * Builds the [PageInfo] for a [MessagesConnection].
     *
     * A `null` [cursor] indicates that the [MessagesConnection] neither [PageInfo.hasNextPage] nor
     * [PageInfo.hasPreviousPage].
     */
    private fun buildPageInfo(chatId: Int, cursor: Cursor?, filter: Filter = null): PageInfo = PageInfo(
        hasNextPage = if (cursor == null) false else hasMessages(chatId, cursor, Chronology.AFTER, filter),
        hasPreviousPage = if (cursor == null) false else hasMessages(chatId, cursor, Chronology.BEFORE, filter),
        startCursor = readCursor(chatId, CursorType.START, filter),
        endCursor = readCursor(chatId, CursorType.END, filter)
    )

    /** Whether the [chatId] has messages [Chronology.BEFORE] or [Chronology.AFTER] the [messageId]. */
    private fun hasMessages(chatId: Int, messageId: Int, chronology: Chronology, filter: Filter = null): Boolean {
        var op: Op<Boolean> = when (chronology) {
            Chronology.BEFORE -> Messages.id less messageId
            Chronology.AFTER -> Messages.id greater messageId
        }
        filter?.let { op = op and it }
        return transaction {
            !select { (Messages.chatId eq chatId) and op }.empty()
        }
    }

    /** The ID of the [type] of message in the [chatId], or `null` if there are no messages. */
    private fun readCursor(chatId: Int, type: CursorType, filter: Filter = null): Int? {
        val order = when (type) {
            CursorType.START -> SortOrder.ASC
            CursorType.END -> SortOrder.DESC
        }
        var op = Messages.chatId eq chatId
        filter?.let { op = op and it }
        return transaction { select(op).orderBy(Messages.id, order).firstOrNull()?.get(Messages.id)?.value }
    }

    /**
     * Whether the [userId] can see the message [id].
     *
     * Returns `true` if the [userId] never deleted the chat. `false` if the message was sent before the [userId]
     * deleted the chat, and `true` otherwise.
     */
    fun isVisible(id: Int, userId: Int): Boolean {
        val deletion = PrivateChatDeletions.readLastDeletion(readChatFromMessage(id), userId) ?: return true
        return read(id).dateTimes.sent >= deletion
    }
}