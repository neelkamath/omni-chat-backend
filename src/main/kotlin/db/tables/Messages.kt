package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.toLinkedHashSet
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

/** Messages checked to exist can optionally be filtered by this [Op]. */
private typealias Filter = Op<Boolean>?

/**
 * @see [TextMessages]
 * @see [ActionMessages]
 * @see [AudioMessages]
 * @see [VideoMessages]
 * @see [DocMessages]
 * @see [PollMessages]
 * @see [GroupChatInviteMessages]
 * @see [PicMessages]
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
        toDb = { PostgresEnum("message_type", it) },
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

    private val isForwarded: Column<Boolean> = bool("is_forwarded")

    private data class ChatAndMessageId(val chatId: Int, val messageId: Int)

    data class RawMessage(
        val type: MessageType,
        val messageId: Int,
        val sender: Account,
        val state: MessageState,
        val sent: LocalDateTime,
        val statuses: List<MessageDateTimeStatus>,
        val context: MessageContext,
        val isForwarded: Boolean,
    ) {
        fun toNewTextMessage(): NewTextMessage = NewTextMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
            TextMessages.read(messageId),
        )

        fun toNewActionMessage(): NewActionMessage = NewActionMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
            ActionMessages.read(messageId),
        )

        fun toNewAudioMessage(): NewAudioMessage = NewAudioMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
        )

        fun toNewGroupChatInviteMessage(): NewGroupChatInviteMessage {
            val chatId = readChatIdFromMessageId(messageId)
            return NewGroupChatInviteMessage(
                chatId,
                messageId,
                sender,
                state,
                sent,
                context,
                isForwarded,
                GroupChats.readInviteCode(chatId),
            )
        }

        fun toNewDocMessage(): NewDocMessage = NewDocMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
        )

        fun toNewVideoMessage(): NewVideoMessage = NewVideoMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
        )

        fun toNewPicMessage(): NewPicMessage = NewPicMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
            PicMessages.read(messageId).caption,
        )

        fun toNewPollMessage(): NewPollMessage = NewPollMessage(
            readChatIdFromMessageId(messageId),
            messageId,
            sender,
            state,
            sent,
            context,
            isForwarded,
            PollMessages.read(messageId),
        )
    }

    private enum class Chronology { BEFORE, FROM }

    /**
     * Returns whether the [chatId] is a broadcast group with the [userId] as an admin. It's assumed the [userId] is in
     * the [chatId]. The [chatId] can be a private or group chat.
     */
    fun isInvalidBroadcast(userId: Int, chatId: Int): Boolean {
        if (chatId in PrivateChats.readIdList(userId)) return false
        val isBroadcast = GroupChats.readChat(
            chatId,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0),
            userId = userId,
        ).isBroadcast
        return isBroadcast && !GroupChatUsers.isAdmin(userId, chatId)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     */
    fun createTextMessage(
        userId: Int,
        chatId: Int,
        message: MessageText,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.TEXT, contextMessageId, isForwarded) { messageId ->
        TextMessages.create(messageId, message)
    }

    fun createActionMessage(
        userId: Int,
        chatId: Int,
        message: ActionMessageInput,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.ACTION, contextMessageId, isForwarded) { messageId ->
        ActionMessages.create(messageId, message)
    }

    fun createPicMessage(
        userId: Int,
        chatId: Int,
        message: CaptionedPic,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.PIC, contextMessageId, isForwarded) { messageId ->
        PicMessages.create(messageId, message)
    }

    fun createGroupChatInviteMessage(
        userId: Int,
        chatId: Int,
        invitedChatId: Int,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.GROUP_CHAT_INVITE, contextMessageId, isForwarded) { messageId ->
        GroupChatInviteMessages.create(messageId, invitedChatId)
    }

    fun createAudioMessage(
        userId: Int,
        chatId: Int,
        message: Audio,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.AUDIO, contextMessageId, isForwarded) { messageId ->
        AudioMessages.create(messageId, message)
    }

    fun createVideoMessage(
        userId: Int,
        chatId: Int,
        message: Mp4,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.VIDEO, contextMessageId, isForwarded) { messageId ->
        VideoMessages.create(messageId, message)
    }

    fun createDocMessage(
        userId: Int,
        chatId: Int,
        message: Doc,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.DOC, contextMessageId, isForwarded) { messageId ->
        DocMessages.create(messageId, message)
    }

    fun createPollMessage(
        userId: Int,
        chatId: Int,
        message: PollInput,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.POLL, contextMessageId, isForwarded) { messageId ->
        PollMessages.create(messageId, message)
    }

    /**
     * Use the `create<TYPE>Message` (e.g., [createTextMessage]) functions instead.
     *
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. An [IllegalArgumentException] will be
     * thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast], or the [contextMessageId] isn't in the
     * [chatId].
     */
    private fun create(
        userId: Int,
        chatId: Int,
        type: MessageType,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
        creator: (messageId: Int) -> Unit,
    ) {
        if (!isUserInChat(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't in the chat (ID: $chatId).")
        if (isInvalidBroadcast(userId, chatId))
            throw IllegalArgumentException("The user (ID: $userId) isn't an admin of the broadcast chat (ID: $chatId).")
        if (contextMessageId != null && contextMessageId !in readIdList(chatId))
            throw IllegalArgumentException(
                "The context message (ID: $contextMessageId) isn't in the chat (ID: $chatId).",
            )
        val row = transaction {
            insert {
                it[this.chatId] = chatId
                it[senderId] = userId
                it[this.type] = type
                it[hasContext] = contextMessageId != null
                it[this.contextMessageId] = contextMessageId
                it[this.isForwarded] = isForwarded
            }.resultedValues!![0]
        }
        creator(row[id].value)
        messagesNotifier.publish(NewMessage.build(row[id].value) as MessagesSubscription, readUserIdList(chatId))
    }

    /**
     * Forwards the [messageId] to the [chatId] by calling the relevant `create<TYPE>Message`
     * (e.g., [createTextMessage]) function.
     */
    fun forward(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int?): Unit = when (readType(messageId)) {
        MessageType.TEXT ->
            createTextMessage(userId, chatId, TextMessages.read(messageId), contextMessageId, isForwarded = true)

        MessageType.ACTION -> createActionMessage(
            userId,
            chatId,
            ActionMessages.read(messageId).toActionMessageInput(),
            contextMessageId,
            isForwarded = true,
        )

        MessageType.PIC ->
            createPicMessage(userId, chatId, PicMessages.read(messageId), contextMessageId, isForwarded = true)

        MessageType.GROUP_CHAT_INVITE -> createGroupChatInviteMessage(
            userId,
            chatId,
            GroupChatInviteMessages.read(messageId),
            contextMessageId,
            isForwarded = true,
        )

        MessageType.AUDIO ->
            createAudioMessage(userId, chatId, AudioMessages.read(messageId), contextMessageId, isForwarded = true)

        MessageType.VIDEO ->
            createVideoMessage(userId, chatId, VideoMessages.read(messageId), contextMessageId, isForwarded = true)

        MessageType.DOC ->
            createDocMessage(userId, chatId, DocMessages.read(messageId), contextMessageId, isForwarded = true)

        MessageType.POLL -> {
            val poll = with(PollMessages.read(messageId)) { PollInput(title, options.map { it.option }) }
            createPollMessage(userId, chatId, poll, contextMessageId, isForwarded = true)
        }
    }

    private fun readType(messageId: Int): MessageType = transaction { select(Messages.id eq messageId).first()[type] }

    /**
     * Case-insensitively [query]s the [chatId]'s [MessageEdge]s as seen by the [userId], or an anonymous user if no
     * [userId] was passed. The returned [MessageEdge]s are chronologically ordered.
     */
    fun searchGroupChat(
        chatId: Int,
        query: String,
        pagination: BackwardPagination? = null,
        userId: Int? = null,
    ): LinkedHashSet<MessageEdge> = search(readGroupChat(chatId, pagination, userId), query)

    /**
     * [query]s the private chat [id]'s [Message]s which haven't been deleted (such as through [PrivateChatDeletions])
     * by the [userId]. The returned [MessageEdge]s are chronologically ordered.
     */
    fun searchPrivateChat(
        chatId: Int,
        userId: Int,
        query: String,
        pagination: BackwardPagination? = null,
    ): LinkedHashSet<MessageEdge> = search(readPrivateChat(userId, chatId, pagination), query)

    /**
     * Case-insensitively [query]s the [edges]s [MessageEdge.node]s. An [IllegalArgumentException] will be thrown if the
     * [edges] [MessageEdge.node] isn't a concrete class.
     *
     * @see [searchGroupChat]
     * @see [searchPrivateChat]
     */
    private fun search(edges: LinkedHashSet<MessageEdge>, query: String): LinkedHashSet<MessageEdge> = edges
        .filter { edge ->
            when (edge.node) {
                is TextMessage -> edge.node.textMessage.value.contains(query, ignoreCase = true)
                is ActionMessage -> {
                    val actionableMessage = ActionMessages.read(edge.node.messageId)
                    actionableMessage.text.value.contains(query, ignoreCase = true) ||
                            actionableMessage.actions.any { it.value.contains(query, ignoreCase = true) }
                }
                is PicMessage -> edge.node.caption?.value?.contains(query, ignoreCase = true) ?: false
                is PollMessage -> {
                    val poll = PollMessages.read(edge.node.messageId)
                    poll.title.value.contains(query, ignoreCase = true) ||
                            poll.options.any { it.option.value.contains(query, ignoreCase = true) }
                }
                is AudioMessage, is GroupChatInviteMessage, is DocMessage, is VideoMessage -> false
                else -> throw IllegalArgumentException("${edge.node} didn't match a concrete class.")
            }
        }
        .toLinkedHashSet()

    /**
     * Returns the [chatId]'s [MessageEdge]s (chronologically ordered) which haven't been deleted (such as through
     * [PrivateChatDeletions]) by the [userId].
     *
     * @see [readPrivateChatConnection]
     */
    fun readPrivateChat(userId: Int, chatId: Int, pagination: BackwardPagination? = null): LinkedHashSet<MessageEdge> {
        val op = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        return readChat(chatId, pagination, op, userId)
    }

    /**
     * Returns the [chatId]'s [MessageEdge]s (chronologically ordered) as seen by the [userId], or an anonymous user if
     * there's no [userId].
     *
     * @see [readGroupChatConnection]
     */
    fun readGroupChat(
        chatId: Int,
        pagination: BackwardPagination? = null,
        userId: Int? = null,
    ): LinkedHashSet<MessageEdge> = readChat(chatId, pagination, userId = userId)

    /**
     * Returns the [chatId]'s [MessageEdge]s as seen by the [userId], or an anonymous user if there's no [userId]. The
     * returned [MessageEdge.node]s are concrete classes sorted chronologically.
     *
     * @see [readPrivateChat]
     * @see [readGroupChat]
     */
    private fun readChat(
        chatId: Int,
        pagination: BackwardPagination? = null,
        filter: Filter = null,
        userId: Int? = null,
    ): LinkedHashSet<MessageEdge> {
        val (last, before) = pagination ?: BackwardPagination()
        var op = this.chatId eq chatId
        before?.let { op = op and (Messages.id less it) }
        filter?.let { op = op and it }
        return transaction {
            select(op)
                .orderBy(Messages.id, SortOrder.DESC)
                .let { if (last == null) it else it.limit(last) }
                .reversed()
                .map { MessageEdge(buildMessage(it, userId), cursor = it[Messages.id].value) }
                .toLinkedHashSet()
        }
    }

    /** The message IDs in the [chatId] in order of creation. */
    fun readIdList(chatId: Int): LinkedHashSet<Int> = transaction {
        select(Messages.chatId eq chatId).orderBy(Messages.id).map { it[Messages.id].value }.toLinkedHashSet()
    }

    /**
     * Returns a concrete class for the [messageId] as seen by the [userId].
     *
     * @see [readRawMessage]
     */
    fun readMessage(userId: Int, messageId: Int): Message {
        val row = transaction { select(Messages.id eq messageId).first() }
        return buildMessage(row, userId)
    }

    /**
     * Returns a concrete class as seen by the [userId], or an anonymous user if there's no [userId].
     *
     * @see [readMessage]
     * @see [readRawMessage]
     */
    private fun buildMessage(row: ResultRow, userId: Int? = null): Message {
        val message = readRawMessage(row[id].value)
        return when (message.type) {
            MessageType.TEXT -> TextMessage.build(message, userId)
            MessageType.ACTION -> ActionMessage.build(message, userId)
            MessageType.PIC -> PicMessage.build(message, userId)
            MessageType.AUDIO -> AudioMessage.build(message, userId)
            MessageType.GROUP_CHAT_INVITE -> GroupChatInviteMessage.build(message, userId)
            MessageType.VIDEO -> VideoMessage.build(message, userId)
            MessageType.DOC -> DocMessage.build(message, userId)
            MessageType.POLL -> PollMessage.build(message, userId)
        }
    }

    /** @see [readMessage] */
    fun readRawMessage(messageId: Int): RawMessage {
        val row = transaction { select(Messages.id eq messageId).first() }
        return RawMessage(
            row[type],
            messageId,
            Users.read(row[senderId]).toAccount(),
            readState(messageId),
            row[sent],
            MessageStatuses.read(messageId).toList(),
            MessageContext(row[hasContext], row[contextMessageId]),
            row[isForwarded],
        )
    }

    private fun readState(messageId: Int): MessageState {
        val count = readUserIdList(readChatIdFromMessageId(messageId)).size - 1 // Subtract 1 to exclude the sender.
        val statuses = MessageStatuses.read(messageId)
        val hasStatus = { status: MessageStatus ->
            statuses.count { it.status == status } == count
        }
        return when {
            hasStatus(MessageStatus.READ) -> MessageState.READ
            hasStatus(MessageStatus.DELIVERED) -> MessageState.DELIVERED
            else -> MessageState.SENT
        }
    }

    /**
     * The ID of the chat which contains the [messageId].
     *
     * @see [Messages.isExistingFrom]
     */
    fun readChatIdFromMessageId(messageId: Int): Int =
        transaction { select(Messages.id eq messageId).first()[chatId] }

    /** [Messages] with [contextMessageId]s of deleted messages will have their [contextMessageId] set to `null`. */
    private fun deleteChatMessages(messageIdList: Collection<Int>) {
        MessageStatuses.delete(messageIdList)
        Stargazers.deleteStars(messageIdList)
        TextMessages.delete(messageIdList)
        ActionMessages.delete(messageIdList)
        PicMessages.delete(messageIdList)
        AudioMessages.delete(messageIdList)
        PollMessages.delete(messageIdList)
        DocMessages.delete(messageIdList)
        VideoMessages.delete(messageIdList)
        transaction {
            update({ contextMessageId inList messageIdList }) { it[contextMessageId] = null }
            deleteWhere { Messages.id inList messageIdList }
        }
    }

    private fun deleteChatMessages(vararg messageIdList: Int): Unit = deleteChatMessages(messageIdList.toList())

    /**
     * Deletes all messages in the [chatId]. Clients will be notified of a [DeletionOfEveryMessage] via
     * [messagesNotifier].
     */
    fun deleteChat(chatId: Int) {
        deleteChatMessages(readMessageIdList(chatId))
        messagesNotifier.publish(DeletionOfEveryMessage(chatId), readUserIdList(chatId))
    }

    /**
     * Deletes all messages in the [chatId] [until] the specified [LocalDateTime]. Subscribers will be notified of the
     * [MessageDeletionPoint] via [messagesNotifier].
     */
    fun deleteChatUntil(chatId: Int, until: LocalDateTime) {
        deleteChatMessages(readMessageIdList(chatId, sent less until))
        messagesNotifier.publish(MessageDeletionPoint(chatId, until), readUserIdList(chatId))
    }

    /**
     * Deletes all messages the [userId] created in the [chatId]. Subscribers will be notified of the
     * [UserChatMessagesRemoval] via [messagesNotifier].
     */
    fun deleteUserChatMessages(chatId: Int, userId: Int) {
        MessageStatuses.deleteUserChatStatuses(chatId, userId)
        deleteChatMessages(readMessageIdList(chatId, senderId eq userId))
        messagesNotifier.publish(UserChatMessagesRemoval(chatId, userId), readUserIdList(chatId))
    }

    /**
     * Deletes all messages the [userId] has, and notifies subscribers of the [UserChatMessagesRemoval] via
     * [messagesNotifier]. Nothing will happen if the [userId] doesn't exist.
     */
    fun deleteUserMessages(userId: Int) {
        MessageStatuses.deleteUserStatuses(userId)
        val chatMessages = readChatMessages(userId)
        deleteChatMessages(chatMessages.map { it.messageId })
        chatMessages.forEach { (chatId) ->
            messagesNotifier.publish(UserChatMessagesRemoval(chatId, userId), readUserIdList(chatId))
        }
    }

    /**
     * Deletes the message [id] in the [chatId] from messages. Subscribers will be notified of the [DeletedMessage] via
     * [messagesNotifier].
     */
    fun delete(id: Int) {
        val chatId = readChatIdFromMessageId(id)
        deleteChatMessages(id)
        messagesNotifier.publish(DeletedMessage(chatId, id), readUserIdList(chatId))
    }

    /**
     * Every [ChatAndMessageId] the [userId] created which are visible to at least one user. Returns an empty list if
     * the [userId] doesn't exist.
     */
    private fun readChatMessages(userId: Int): Set<ChatAndMessageId> = transaction {
        select(senderId eq userId).map { ChatAndMessageId(it[chatId], it[Messages.id].value) }.toSet()
    }

    /** Whether there are messages in the [chatId] [from] the [LocalDateTime]. */
    fun isExistingFrom(chatId: Int, from: LocalDateTime): Boolean =
        transaction { select((Messages.chatId eq chatId) and (sent greaterEq from)).empty().not() }

    /** The [id] list for the [chatId]. */
    private fun readMessageIdList(chatId: Int, filter: Filter = null): Set<Int> = transaction {
        val chatOp = Messages.chatId eq chatId
        val op = if (filter == null) chatOp else chatOp and filter
        select(op).map { it[Messages.id].value }.toSet()
    }

    fun isExisting(id: Int): Boolean = transaction { select(Messages.id eq id).empty().not() }

    /** The [MessagesConnection] as seen by the [userId], or an anonymous user if there's no [userId]. */
    fun readGroupChatConnection(
        chatId: Int,
        pagination: BackwardPagination? = null,
        userId: Int? = null,
    ): MessagesConnection {
        val edges = readGroupChat(chatId, pagination, userId).toList()
        val pageInfo = buildPageInfo(chatId, edges.firstOrNull()?.cursor, pagination)
        return MessagesConnection(edges, pageInfo)
    }

    fun readPrivateChatConnection(
        chatId: Int,
        userId: Int,
        pagination: BackwardPagination? = null,
    ): MessagesConnection {
        val op = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        val edges = readPrivateChat(userId, chatId, pagination).toList()
        val pageInfo = buildPageInfo(chatId, edges.firstOrNull()?.cursor, pagination, op)
        return MessagesConnection(edges, pageInfo)
    }

    /** Builds the [MessagesConnection.pageInfo]. The [firstMessageId] is the ID of the message. */
    private fun buildPageInfo(
        chatId: Int,
        firstMessageId: Int?,
        pagination: BackwardPagination? = null,
        filter: Filter = null,
    ): PageInfo {
        val hasNextPage =
            if (pagination?.before == null) false else hasMessages(chatId, pagination.before, Chronology.FROM, filter)
        val startCursor = readCursor(chatId, CursorType.START, filter)
        val endCursor = readCursor(chatId, CursorType.END, filter)
        val hasPreviousPage = when {
            firstMessageId != null -> hasMessages(chatId, firstMessageId, Chronology.BEFORE, filter)
            pagination?.before == null ->
                if (endCursor == null) false else hasMessages(chatId, endCursor, Chronology.BEFORE, filter)
            else -> hasMessages(chatId, pagination.before, Chronology.BEFORE, filter)
        }
        return PageInfo(hasNextPage, hasPreviousPage, startCursor, endCursor)
    }

    /** Whether the [chatId] has messages [Chronology.BEFORE] or [Chronology.FROM] the [messageId]. */
    private fun hasMessages(chatId: Int, messageId: Int, chronology: Chronology, filter: Filter = null): Boolean {
        var op: Op<Boolean> = when (chronology) {
            Chronology.BEFORE -> Messages.id less messageId
            Chronology.FROM -> Messages.id greaterEq messageId
        }
        filter?.let { op = op and it }
        return transaction { select((Messages.chatId eq chatId) and op).empty().not() }
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
     * - `true` if the [messageId] is from a public group chat (regardless of whether the [userId] is a participant).
     * - `false` if the [userId] is `null`, and the [messageId] isn't from a public group chat.
     * - `false` if the [messageId] is from a chat the user isn't in.
     * - `true` if the [messageId] is from a group chat the [userId] is in.
     * - `true` if the [messageId] is from a private chat the [userId] hasn't deleted.
     * - `false` if the [messageId] was sent before the [userId] deleted the private chat.
     */
    fun isVisible(userId: Int?, messageId: Int): Boolean {
        if (!isExisting(messageId)) return false
        val chatId = readChatIdFromMessageId(messageId)
        if (GroupChats.isExistentPublicChat(chatId)) return true
        if (userId == null) return false
        if (!isUserInChat(userId, chatId)) return false
        if (chatId in GroupChatUsers.readChatIdList(userId)) return true
        val deletion = PrivateChatDeletions.readLastDeletion(chatId, userId) ?: return true
        return readMessage(userId, messageId).sent >= deletion
    }
}
