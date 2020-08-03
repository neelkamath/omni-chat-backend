package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.readUserById
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
 * @see [TextMessages]
 * @see [AudioMessages]
 * @see [PollMessages]
 * @see [CaptionedPic]
 * @see [Stargazers]
 * @see [MessageStatuses]
 */
object Messages : IntIdTable() {
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val sent: Column<LocalDateTime> = datetime("sent").clientDefault { LocalDateTime.now() }
    private val senderId: Column<Int> = integer("sender_id").references(Users.id)
    private val type: Column<MessageType> = customEnumeration(
        name = "type",
        sql = "message_type",
        fromDb = { MessageType.valueOf((it as String).toUpperCase()) },
        toDb = { PostgresEnum("message_type", it) }
    )

    /** Whether this message was in reply to a particular message. */
    private val hasContext: Column<Boolean> = bool("has_context")

    /**
     * The ID of the message this message is in reply to.
     *
     * If not [hasContext], this will be `null`. If [hasContext], this will be `null` only if the message being replied
     * to was deleted.
     */
    private val contextMessageId: Column<Int?> = integer("context_message_id").references(Messages.id).nullable()

    private data class ChatAndMessageId(val chatId: Int, val messageId: Int)

    data class TypedMessage(val type: MessageType, val message: BareMessage)

    private enum class CursorType {
        /** First message's cursor. */
        START,

        /** Last message's cursor. */
        END
    }

    /** Whether messages exist before or after a particular point in time. */
    private enum class Chronology { BEFORE, AFTER }

    /**
     * Returns whether the [chatId] is a broadcast group with the [userId] as an admin. It's assumed the [userId] is in
     * the [chatId]. The [chatId] needn't be a broadcast group.
     */
    fun isInvalidBroadcast(userId: Int, chatId: Int): Boolean {
        if (chatId in PrivateChats.readIdList(userId)) return false
        val isBroadcast = GroupChats.readChat(
            userId,
            chatId,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0)
        ).isBroadcast
        return isBroadcast && !GroupChatUsers.isAdmin(userId, chatId)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesBroker].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], or if [isInvalidBroadcast].
     */
    fun create(userId: Int, chatId: Int, message: MessageText, contextMessageId: Int?): Unit =
        create(userId, chatId, MessageType.TEXT, contextMessageId) { messageId ->
            TextMessages.create(messageId, message)
        }

    fun create(userId: Int, chatId: Int, message: CaptionedPic, contextMessageId: Int?): Unit =
        create(userId, chatId, MessageType.PIC, contextMessageId) { messageId ->
            PicMessages.create(messageId, message)
        }

    fun create(userId: Int, chatId: Int, message: Mp3, contextMessageId: Int?): Unit =
        create(userId, chatId, MessageType.AUDIO, contextMessageId) { messageId ->
            AudioMessages.create(messageId, message)
        }

    fun create(userId: Int, chatId: Int, message: PollInput, contextMessageId: Int?): Unit =
        create(userId, chatId, MessageType.POLL, contextMessageId) { messageId ->
            PollMessages.create(messageId, message)
        }

    private fun create(
        userId: Int,
        chatId: Int,
        type: MessageType,
        contextMessageId: Int?,
        creator: (messageId: Int) -> Unit
    ) {
        if (!isUserInChat(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't in the chat (ID: $chatId).")
        if (isInvalidBroadcast(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't an admin of the broadcast chat (ID: $chatId).")
        val row = transaction {
            insert {
                it[this.chatId] = chatId
                it[senderId] = userId
                it[this.type] = type
                it[hasContext] = contextMessageId != null
                it[this.contextMessageId] = contextMessageId
            }.resultedValues!![0]
        }
        creator(row[id].value)
        val message = NewMessage.build(row[id].value) as MessagesSubscription
        messagesBroker.notify(message) { isUserInChat(it.userId, chatId) }
    }

    /** Case-insensitively [query]s the [chatId]'s messages as seen by the [userId]. */
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
     * Case-insensitively [query]s [MessageEdge.node]s. An [IllegalArgumentException] will be thrown if the [edges]
     * [MessageEdge.node] isn't a concrete class.
     *
     * @see [searchGroupChat]
     * @see [searchPrivateChat]
     */
    private fun search(edges: List<MessageEdge>, query: String): List<MessageEdge> = edges.filter { edge ->
        when (edge.node) {
            is TextMessage -> edge.node.message.value.contains(query, ignoreCase = true)
            is PicMessage -> edge.node.caption?.value?.contains(query, ignoreCase = true) ?: false
            is PollMessage -> {
                val poll = PollMessages.read(edge.node.messageId)
                poll.title.value.contains(query, ignoreCase = true) ||
                        poll.options.any { it.option.value.contains(query, ignoreCase = true) }
            }
            is AudioMessage -> false
            else -> throw IllegalArgumentException("${edge.node} didn't match a concrete class.")
        }
    }

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
     * The [userId] reading the [chatId]'s [MessageEdge]s. The returned [MessageEdge]s are concrete classes.
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
                .map { MessageEdge(buildMessage(userId, it), cursor = it[Messages.id].value) }
        }
    }

    /** The message IDs in the [chatId] in order of creation. */
    fun readIdList(chatId: Int): List<Int> = transaction {
        select { Messages.chatId eq chatId }.map { it[Messages.id].value }
    }

    /** Returns a concrete class for the [messageId] as seen by the [userId]. */
    fun readMessage(userId: Int, messageId: Int): Message {
        val row = transaction {
            select { Messages.id eq messageId }.first()
        }
        return buildMessage(userId, row)
    }

    /**
     * Returns a concrete class as seen by the [userId].
     *
     * @see [readMessage]
     * @see [readTypedMessage]
     */
    private fun buildMessage(userId: Int, row: ResultRow): Message {
        val (type, message) = readTypedMessage(row[id].value)
        return when (type) {
            MessageType.TEXT -> TextMessage.build(userId, message)
            MessageType.PIC -> PicMessage.build(userId, message)
            MessageType.AUDIO -> AudioMessage.build(userId, message)
            MessageType.POLL -> PollMessage.build(userId, message)
        }
    }

    /** @see [readMessage] */
    fun readTypedMessage(messageId: Int): TypedMessage {
        val row = transaction {
            select { Messages.id eq messageId }.first()
        }
        val message = object : BareMessage {
            override val messageId: Int = messageId
            override val sender: Account = readUserById(row[senderId])
            override val dateTimes: MessageDateTimes = MessageDateTimes(row[sent], MessageStatuses.read(messageId))
            override val context: MessageContext = MessageContext(row[hasContext], row[contextMessageId])
        }
        return TypedMessage(row[type], message)
    }

    /**
     * The ID of the chat which contains the [messageId].
     *
     * @see [Messages.exists]
     */
    fun readChatFromMessage(messageId: Int): Int = transaction {
        select { Messages.id eq messageId }.first()[chatId]
    }

    /** [Messages] with [contextMessageId]s of deleted messages will have their [contextMessageId] set to `null`. */
    private fun deleteChatMessages(messageIdList: List<Int>) {
        MessageStatuses.delete(messageIdList)
        Stargazers.deleteStars(messageIdList)
        TextMessages.delete(messageIdList)
        PicMessages.delete(messageIdList)
        AudioMessages.delete(messageIdList)
        PollMessages.delete(messageIdList)
        transaction {
            update({ contextMessageId inList messageIdList }) { it[contextMessageId] = null }
            deleteWhere { Messages.id inList messageIdList }
        }
    }

    private fun deleteChatMessages(vararg messageIdList: Int): Unit = deleteChatMessages(messageIdList.toList())

    /**
     * Deletes all messages in the [chatId]. Clients will be notified of a [DeletionOfEveryMessage] via
     * [messagesBroker].
     */
    fun deleteChat(chatId: Int) {
        deleteChatMessages(readMessageIdList(chatId))
        messagesBroker.notify(DeletionOfEveryMessage(chatId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all messages in the [chatId] [until] the specified [LocalDateTime]. Subscribers will be notified of the
     * [MessageDeletionPoint] via [messagesBroker].
     */
    fun deleteChatUntil(chatId: Int, until: LocalDateTime) {
        deleteChatMessages(readMessageIdList(chatId, sent less until))
        messagesBroker.notify(MessageDeletionPoint(chatId, until)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all messages the [userId] created in the [chatId]. Subscribers will be notified of the
     * [UserChatMessagesRemoval] via [messagesBroker].
     */
    fun deleteUserChatMessages(chatId: Int, userId: Int) {
        MessageStatuses.deleteUserChatStatuses(chatId, userId)
        deleteChatMessages(readMessageIdList(chatId, senderId eq userId))
        messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes all messages the [userId] has. Subscribers will be notified of the [UserChatMessagesRemoval] via
     * [messagesBroker].
     */
    fun deleteUserMessages(userId: Int) {
        MessageStatuses.deleteUserStatuses(userId)
        val chatMessages = readChatMessages(userId)
        deleteChatMessages(chatMessages.map { it.messageId })
        for ((chatId) in chatMessages)
            messagesBroker.notify(UserChatMessagesRemoval(chatId, userId)) { isUserInChat(it.userId, chatId) }
    }

    /**
     * Deletes the message [id] in the [chatId] from messages. Subscribers will be notified of the [DeletedMessage] via
     * [messagesBroker].
     */
    fun delete(id: Int) {
        val chatId = readChatFromMessage(id)
        deleteChatMessages(id)
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
        select { Messages.id eq id }.empty().not()
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