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

/**
 * @see [Stargazers]
 * @see [MessageStatuses]
 */
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
    fun create(userId: Int, chatId: Int, text: TextMessage) {
        if (!isUserInChat(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't in the chat (ID: $chatId).")
        val row = transaction {
            insert {
                it[this.chatId] = chatId
                it[this.text] = text.value
                it[senderId] = userId
            }.resultedValues!![0]
        }
        val message = NewMessage.build(row[id].value, buildMessage(row))
        messagesBroker.notify(message) { isUserInChat(it.userId, chatId) }
    }

    /** Case-insensitively [query]s the [chatId]'s messages for the [userId]. */
    fun searchGroupChat(
        userId: Int,
        chatId: Int,
        query: String,
        pagination: BackwardPagination? = null
    ): List<MessageEdge> = search(readGroupChat(userId, chatId, pagination), query)

    /**
     * [query]s the private chat [id]'s [Message]s which haven't been deleted (such as through [PrivateChatDeletions])
     * by the [userId].
     */
    fun searchPrivateChat(
        chatId: Int,
        userId: Int,
        query: String,
        pagination: BackwardPagination? = null
    ): List<MessageEdge> = search(readPrivateChat(userId, chatId, pagination), query)

    /**
     * Case-insensitively [query]s [MessageEdge.node]s.
     *
     * @see [searchGroupChat]
     * @see [searchPrivateChat]
     */
    private fun search(edges: List<MessageEdge>, query: String): List<MessageEdge> =
        edges.filter { it.node.text.value.contains(query, ignoreCase = true) }

    /**
     * The [chatId]'s [Message]s which haven't been deleted (such as through [PrivateChatDeletions]) by the [userId].
     *
     * @see [readPrivateChatConnection]
     */
    fun readPrivateChat(userId: Int, chatId: Int, pagination: BackwardPagination? = null): List<MessageEdge> {
        val op = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        return readChat(userId, chatId, pagination, op)
    }

    /**
     * The [userId]'s [chatId]'s [MessageEdge]s.
     *
     * @see [readGroupChatConnection]
     */
    fun readGroupChat(userId: Int, chatId: Int, pagination: BackwardPagination? = null): List<MessageEdge> =
        readChat(userId, chatId, pagination)

    /**
     * The [userId] reading the [chatId]'s [MessageEdge]s.
     *
     * @see [readPrivateChat]
     * @see [readGroupChat]
     */
    private fun readChat(
        userId: Int,
        chatId: Int,
        pagination: BackwardPagination? = null,
        filter: Filter = null
    ): List<MessageEdge> {
        val (last, before) = pagination ?: BackwardPagination()
        var op = this.chatId eq chatId
        before?.let { op = op and (Messages.id less it) }
        filter?.let { op = op and it }
        return transaction {
            select(op)
                .orderBy(Messages.id, SortOrder.DESC)
                .let { if (last == null) it else it.limit(last) }
                .reversed()
                .map {
                    val messageId = it[Messages.id].value
                    val message = Message.build(userId, messageId, buildMessage(it))
                    MessageEdge(message, cursor = messageId)
                }
        }
    }

    /** The message IDs in the [chatId] in order of creation. */
    fun readIdList(chatId: Int): List<Int> = transaction {
        select { Messages.chatId eq chatId }.map { it[Messages.id].value }
    }

    /**
     * Returns the [messageId] for the [userId].
     *
     * @see [readBareMessage]
     */
    fun readMessage(userId: Int, messageId: Int): Message = Message.build(userId, messageId, readBareMessage(messageId))

    /**
     * The ID of the chat which contains the [messageId].
     *
     * @see [Messages.exists]
     */
    fun readChatFromMessage(messageId: Int): Int = transaction {
        select { Messages.id eq messageId }.first()[chatId]
    }

    /**
     * Deletes all [Messages], [MessageStatuses], and [Stargazers] in the [chatId]. Clients will be notified of a
     * [DeletionOfEveryMessage] via [messagesBroker].
     */
    fun deleteChat(chatId: Int) {
        val messageIdList = readMessageIdList(chatId)
        MessageStatuses.delete(messageIdList)
        Stargazers.deleteStars(messageIdList)
        transaction {
            deleteWhere { Messages.chatId eq chatId }
        }
        messagesBroker.notify(DeletionOfEveryMessage(chatId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all [Messages], [MessageStatuses], and [Stargazers] in the [chatId] [until] the specified
     * [LocalDateTime]. [Broker.subscribe]rs will be notified of the [MessageDeletionPoint] via [messagesBroker].
     */
    fun deleteMessagesUntil(chatId: Int, until: LocalDateTime) {
        val idList = readMessageIdList(chatId, sent less until)
        MessageStatuses.delete(idList)
        Stargazers.deleteStars(idList)
        transaction {
            deleteWhere { (Messages.chatId eq chatId) and (sent less until) }
        }
        messagesBroker.notify(MessageDeletionPoint(chatId, until)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all [Messages], [MessageStatuses], and [Stargazers] the [userId] created in the [chatId].
     * [Broker.subscribe]rs will be [Broker.notify]d of the [UserChatMessagesRemoval] via [messagesBroker].
     */
    fun deleteUserChatMessages(chatId: Int, userId: Int) {
        MessageStatuses.deleteUserChatStatuses(chatId, userId)
        val idList = readMessageIdList(chatId, senderId eq userId)
        MessageStatuses.delete(idList)
        Stargazers.deleteStars(idList)
        transaction {
            deleteWhere { Messages.id inList idList }
        }
        messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all [Messages], [MessageStatuses], and [Stargazers] the [userId] has. [Broker.subscribe]rs will be
     * [Broker.notify]d of the [UserChatMessagesRemoval] via [messagesBroker].
     */
    fun deleteUserMessages(userId: Int) {
        MessageStatuses.deleteUserStatuses(userId)
        val chatMessages = readChatMessages(userId)
        val messageIdList = chatMessages.map { it.messageId }
        MessageStatuses.delete(messageIdList)
        Stargazers.deleteStars(messageIdList)
        transaction {
            deleteWhere { senderId eq userId }
        }
        for ((chatId) in chatMessages)
            messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes the message [id] in the [chatId] from [Messages], [MessageStatuses], and [Stargazers].
     * [Broker.subscribe]rs will be [Broker.notify]d of the [DeletedMessage] via [messagesBroker].
     */
    fun delete(id: Int) {
        MessageStatuses.delete(id)
        Stargazers.deleteStar(id)
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

    fun exists(id: Int): Boolean = transaction {
        !select { Messages.id eq id }.empty()
    }

    /** @see [readMessage] */
    fun readBareMessage(messageId: Int): BareMessage = transaction {
        select { Messages.id eq messageId }.first().let(::buildMessage)
    }

    /** @see [readBareMessage] */
    private fun buildMessage(row: ResultRow): BareMessage = object : BareMessage {
        override val sender: Account = readUserById(row[senderId])
        override val text: TextMessage = TextMessage(row[Messages.text])
        override val dateTimes: MessageDateTimes = MessageDateTimes(row[sent], MessageStatuses.read(row[id].value))
    }

    fun readGroupChatConnection(userId: Int, chatId: Int, pagination: BackwardPagination? = null): MessagesConnection =
        MessagesConnection(readGroupChat(userId, chatId, pagination), buildPageInfo(chatId, pagination?.before))

    fun readPrivateChatConnection(
        chatId: Int,
        userId: Int,
        pagination: BackwardPagination? = null
    ): MessagesConnection {
        val op = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        return MessagesConnection(
            readPrivateChat(userId, chatId, pagination),
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
     * Whether the [userId] can see the [messageId].
     *
     * @return
     * - `false` if the [messageId] doesn't exist.
     * - `false` if the [messageId] is from a chat the user isn't in.
     * - `true` if the [messageId] is from a group chat the [userId] is in.
     * - `true` if the [messageId] is from a private chat the [userId] hasn't deleted.
     * - `false` if the [messageId] was sent before the [userId] deleted the private chat.
     */
    fun isVisible(userId: Int, messageId: Int): Boolean {
        if (!exists(messageId)) return false
        val chatId = readChatFromMessage(messageId)
        if (!isUserInChat(userId, chatId)) return false
        if (chatId in GroupChatUsers.readChatIdList(userId)) return true
        val deletion = PrivateChatDeletions.readLastDeletion(chatId, userId) ?: return true
        return readMessage(userId, messageId).dateTimes.sent >= deletion
    }
}