package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.`java-time`.datetime
import java.time.LocalDateTime

/** Messages checked to exist can optionally be filtered by this [Op]. */
private typealias Filter = Op<Boolean>?

/**
 * @throws [IllegalArgumentException] if the [value] isn't 1-[Messages.MAX_TEXT_LENGTH] characters with at least one
 * non-whitespace.
 */
data class TextMessage(val value: String) {
    init {
        if (value.trim().isEmpty() || value.length > Messages.MAX_TEXT_LENGTH)
            throw IllegalArgumentException(
                "The text must be 1-${Messages.MAX_TEXT_LENGTH} characters, with at least one non-whitespace."
            )
    }
}

/**
 * The messages for [PrivateChats] and [GroupChats]. The date and time messages were delivered and read are stored in
 * [MessageStatuses].
 *
 * @see [messagesBroker]
 */
object Messages : IntIdTable() {
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val sent: Column<LocalDateTime> = datetime("sent").clientDefault { LocalDateTime.now() }
    private val senderId: Column<String> = varchar("sender_id", USER_ID_LENGTH)

    /** Text messages cannot exceed this length. */
    const val MAX_TEXT_LENGTH = 10_000

    /** Can have at most [MAX_TEXT_LENGTH]. */
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
    fun create(chatId: Int, userId: String, text: TextMessage) {
        if (!isUserInChat(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't in the chat (ID: $chatId).")
        val row = transact {
            insert {
                it[this.chatId] = chatId
                it[Messages.text] = text.value
                it[senderId] = userId
            }.resultedValues!![0]
        }
        val message = NewMessage.build(buildMessage(row))
        messagesBroker.notify(message) { isUserInChat(it.userId, chatId) }
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
        userId: String,
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
     * Returns the private chat [id]'s [Message]s which haven't been deleted (such as through [PrivateChatDeletions]) by
     * the [userId].
     *
     * @see [readPrivateChatConnection]
     */
    fun readPrivateChat(id: Int, userId: String, pagination: BackwardPagination? = null): List<MessageEdge> {
        val op = PrivateChatDeletions.readLastDeletion(id, userId)?.let { sent greater it }
        return readChat(id, pagination, op)
    }

    /** @see [readGroupChatConnection] */
    fun readGroupChat(id: Int, pagination: BackwardPagination? = null): List<MessageEdge> =
        readChat(id, pagination)

    /**
     * Returns the [MessageEdge]s in the chat [id].
     *
     * @see [readPrivateChat]
     * @see [readGroupChat]
     */
    private fun readChat(id: Int, pagination: BackwardPagination? = null, filter: Filter = null): List<MessageEdge> {
        val (last, before) = pagination ?: BackwardPagination()
        var op = chatId eq id
        if (before != null) op = op and (Messages.id less before)
        if (filter != null) op = op and filter
        return transact {
            select(op)
                .orderBy(Messages.id, SortOrder.DESC)
                .let { if (last == null) it else it.limit(last) }
                .reversed()
                .map { MessageEdge(buildMessage(it), cursor = it[Messages.id].value) }
        }
    }

    /** Returns the message IDs in the [chatId]. */
    fun readIdList(chatId: Int): List<Int> = transact {
        select { Messages.chatId eq chatId }.map { it[Messages.id].value }
    }

    fun read(id: Int): Message = transact {
        select { Messages.id eq id }.first().let(::buildMessage)
    }

    /**
     * Returns the ID of the chat which contains the [messageId].
     *
     * @see [Messages.exists]
     */
    fun readChatFromMessage(messageId: Int): Int = transact {
        select { Messages.id eq messageId }.first()[chatId]
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] in the [chatId]. Clients will be notified of a
     * [DeletionOfEveryMessage] via [messagesBroker].
     */
    fun deleteChat(chatId: Int) {
        val messageIdList = readMessageIdList(chatId)
        MessageStatuses.delete(messageIdList)
        transact {
            deleteWhere { Messages.chatId eq chatId }
        }
        messagesBroker.notify(DeletionOfEveryMessage(chatId)) { isUserInChat(it.userId, chatId) }
    }

    /** Deletes all [Messages] and [MessageStatuses] in the [chatId] [until] the specified [LocalDateTime]. */
    fun deleteChatMessagesUntil(chatId: Int, until: LocalDateTime) {
        val idList = readMessageIdList(chatId, sent less until)
        MessageStatuses.delete(idList)
        transact {
            deleteWhere { (Messages.chatId eq chatId) and (sent less until) }
        }
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] the [userId] created in the [chatId]. Clients who have
     * [Broker.subscribe]d to [MessagesSubscription]s via [messagesBroker] will be be notified of the
     * [UserChatMessagesRemoval].
     */
    fun deleteUserChatMessages(chatId: Int, userId: String) {
        MessageStatuses.deleteUserChatStatuses(chatId, userId)
        val idList = readMessageIdList(chatId, senderId eq userId)
        MessageStatuses.delete(idList)
        transact {
            deleteWhere { Messages.id inList idList }
        }
        messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all [Messages] and [MessageStatuses] the [userId] has. Clients who have [Broker.subscribe]d to
     * [MessagesSubscription]s via [messagesBroker] will be notified of the [UserChatMessagesRemoval].
     */
    fun deleteUserMessages(userId: String) {
        MessageStatuses.deleteUserStatuses(userId)
        val chatMessages = readChatMessages(userId)
        MessageStatuses.delete(chatMessages.map { it.messageId })
        transact {
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
        transact {
            deleteWhere { Messages.id eq id }
        }
        messagesBroker.notify(DeletedMessage(chatId, id)) { isUserInChat(it.userId, chatId) }
    }

    /** Returns every [ChatAndMessageId] the [userId] created which are visible to at least one user. */
    private fun readChatMessages(userId: String): List<ChatAndMessageId> =
        transact {
            select { senderId eq userId }.map { ChatAndMessageId(it[chatId], it[Messages.id].value) }
        }

    /** Whether there are messages in the [chatId] [from] the [LocalDateTime]. */
    fun existsFrom(chatId: Int, from: LocalDateTime): Boolean = transact {
        !select { (Messages.chatId eq chatId) and (sent greaterEq from) }.empty()
    }

    /** Returns the [id] list for the [chatId]. */
    private fun readMessageIdList(chatId: Int, filter: Filter = null): List<Int> =
        transact {
            val chatOp = Messages.chatId eq chatId
            val op = if (filter == null) chatOp else chatOp and filter
            select(op).map { it[Messages.id].value }
        }

    /** Whether the [messageId] exists in the [chatId]. */
    fun existsInChat(messageId: Int, chatId: Int): Boolean = transact {
        !select { (Messages.chatId eq chatId) and (Messages.id eq messageId) }.empty()
    }

    fun exists(id: Int): Boolean = transact {
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
        userId: String,
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
        if (filter != null) op = op and filter
        return transact {
            !select { (Messages.chatId eq chatId) and op }.empty()
        }
    }

    /** Returns the ID of the [type] of message in the [chatId], or `null` if there are no messages. */
    private fun readCursor(chatId: Int, type: CursorType, filter: Filter = null): Int? {
        val order = when (type) {
            CursorType.START -> SortOrder.ASC
            CursorType.END -> SortOrder.DESC
        }
        var op = Messages.chatId eq chatId
        if (filter != null) op = op and filter
        return transact { select(op).orderBy(Messages.id, order).firstOrNull()?.get(Messages.id)?.value }
    }

    /**
     * Whether the [userId] can see the message [id].
     *
     * @return `true` if the [userId] never deleted the chat. `false` if the message was sent before the [userId]
     * deleted the chat, and `true` otherwise.
     */
    fun isVisible(id: Int, userId: String): Boolean {
        val deletion = PrivateChatDeletions.readLastDeletion(readChatFromMessage(id), userId) ?: return true
        return read(id).dateTimes.sent >= deletion
    }
}