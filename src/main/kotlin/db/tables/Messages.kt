package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.graphql.routing.ActionMessageInput
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.graphql.routing.PollInput
import com.neelkamath.omniChatBackend.toLinkedHashSet
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
 * @see TextMessages
 * @see ActionMessages
 * @see AudioMessages
 * @see VideoMessages
 * @see DocMessages
 * @see PollMessages
 * @see GroupChatInviteMessages
 * @see ImageMessages
 * @see Bookmarks
 */
object Messages : IntIdTable() {
    private val chatId: Column<Int> = integer("chat_id").references(Chats.id)
    private val sent: Column<LocalDateTime> = datetime("sent").clientDefault { LocalDateTime.now() }
    private val senderId: Column<Int> = integer("sender_id").references(Users.id)
    private val type: Column<MessageType> = customEnumeration(
        name = "type",
        sql = "message_type",
        fromDb = { MessageType.valueOf((it as String).uppercase()) },
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

    /**
     * Returns whether the [chatId] is a broadcast group with the [userId] as an admin. It's assumed the [userId] is in
     * the [chatId]. The [chatId] can be a private or group chat.
     */
    fun isInvalidBroadcast(userId: Int, chatId: Int): Boolean {
        if (chatId in PrivateChats.readIdList(userId)) return false
        val isBroadcast = GroupChats.isBroadcastChat(chatId)
        return isBroadcast && !GroupChatUsers.isAdmin(userId, chatId)
    }

    fun readSenderId(messageId: Int): Int = transaction { select(Messages.id eq messageId).first()[senderId] }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
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

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
    fun createActionMessage(
        userId: Int,
        chatId: Int,
        message: ActionMessageInput,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.ACTION, contextMessageId, isForwarded) { messageId ->
        ActionMessages.create(messageId, message)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
    fun createImageMessage(
        userId: Int,
        chatId: Int,
        message: CaptionedImage,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.IMAGE, contextMessageId, isForwarded) { messageId ->
        ImageMessages.create(messageId, message)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
    fun createGroupChatInviteMessage(
        userId: Int,
        chatId: Int,
        invitedChatId: Int,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.GROUP_CHAT_INVITE, contextMessageId, isForwarded) { messageId ->
        GroupChatInviteMessages.create(messageId, invitedChatId)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
    fun createAudioMessage(
        userId: Int,
        chatId: Int,
        message: AudioFile,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.AUDIO, contextMessageId, isForwarded) { messageId ->
        AudioMessages.create(messageId, message)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
    fun createVideoMessage(
        userId: Int,
        chatId: Int,
        message: VideoFile,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.VIDEO, contextMessageId, isForwarded) { messageId ->
        VideoMessages.create(messageId, message)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
    fun createDocMessage(
        userId: Int,
        chatId: Int,
        message: DocFile,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
    ): Unit = create(userId, chatId, MessageType.DOC, contextMessageId, isForwarded) { messageId ->
        DocMessages.create(messageId, message)
    }

    /**
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see forward
     */
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
     * Subscribers will be notified of the [NewMessage] via [messagesNotifier]. If the [chatId] is a public group chat,
     * subscribers will be notified via [chatMessagesNotifier].
     *
     * An [IllegalArgumentException] will be thrown if the [userId] isn't in the [chatId], if it [isInvalidBroadcast],
     * or the [contextMessageId] isn't in the [chatId].
     *
     * @see createTextMessage
     * @see createActionMessage
     * @see createImageMessage
     * @see createGroupChatInviteMessage
     * @see createAudioMessage
     * @see createVideoMessage
     * @see createDocMessage
     * @see createPollMessage
     * @see forward
     */
    private fun create(
        userId: Int,
        chatId: Int,
        type: MessageType,
        contextMessageId: Int?,
        isForwarded: Boolean = false,
        creator: (messageId: Int) -> Unit,
    ) {
        require(isUserInChat(userId, chatId)) { "The user (ID: $userId) isn't in the chat (ID: $chatId)." }
        require(!isInvalidBroadcast(userId, chatId)) {
            "The user (ID: $userId) isn't an admin of the broadcast chat (ID: $chatId)."
        }
        require(contextMessageId == null || isExistingChatMessage(chatId, contextMessageId)) {
            "The context message (ID: $contextMessageId) isn't in the chat (ID: $chatId)."
        }
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
        val message = when (type) {
            MessageType.TEXT -> NewTextMessage(row[id].value)
            MessageType.ACTION -> NewActionMessage(row[id].value)
            MessageType.AUDIO -> NewAudioMessage(row[id].value)
            MessageType.DOC -> NewDocMessage(row[id].value)
            MessageType.GROUP_CHAT_INVITE -> NewGroupChatInviteMessage(row[id].value)
            MessageType.IMAGE -> NewImageMessage(row[id].value)
            MessageType.POLL -> NewPollMessage(row[id].value)
            MessageType.VIDEO -> NewVideoMessage(row[id].value)
        }
        messagesNotifier.publish(message, readUserIdList(chatId).map(::UserId))
        if (GroupChats.isExistingPublicChat(chatId)) chatMessagesNotifier.publish(message, ChatId(chatId))
    }

    /**
     * Forwards the [messageId] to the [chatId] by calling one of [createTextMessage], [createActionMessage],
     * [createImageMessage], [createGroupChatInviteMessage], [createAudioMessage], [createVideoMessage],
     * [createDocMessage], or [createPollMessage].
     */
    fun forward(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int?): Unit = when (readType(messageId)) {
        MessageType.TEXT ->
            createTextMessage(userId, chatId, TextMessages.read(messageId), contextMessageId, isForwarded = true)

        MessageType.ACTION -> {
            val message =
                ActionMessageInput(ActionMessages.readText(messageId), ActionMessageActions.read(messageId).toList())
            createActionMessage(userId, chatId, message, contextMessageId, isForwarded = true)
        }

        MessageType.IMAGE ->
            createImageMessage(
                userId,
                chatId,
                ImageMessages.readCaptionedImage(messageId),
                contextMessageId,
                isForwarded = true
            )

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
            val poll =
                PollInput(PollMessages.readQuestion(messageId), PollMessageOptions.readOptions(messageId).toList())
            createPollMessage(userId, chatId, poll, contextMessageId, isForwarded = true)
        }
    }

    fun readType(messageId: Int): MessageType = transaction { select(Messages.id eq messageId).first()[type] }

    /**
     * Case-insensitively [query]s the [chatId]'s messages. Returns the IDs of the matched messages sorted in ascending
     * order.
     */
    fun searchGroupChat(chatId: Int, query: String, pagination: BackwardPagination? = null): LinkedHashSet<Int> =
        search(readGroupChat(chatId), query, pagination)

    /**
     * Case-insensitively [query]s the private chat [id]'s messages which haven't been deleted (such as through
     * [PrivateChatDeletions]) by the [userId]. Returns the IDs of the matched messages sorted in ascending order .
     */
    fun searchPrivateChat(
        chatId: Int,
        userId: Int,
        query: String,
        pagination: BackwardPagination? = null,
    ): LinkedHashSet<Int> = search(readPrivateChat(userId, chatId), query, pagination)

    /**
     * Case-insensitively [query]s the messages. Returns the IDs of the matched messages sorted in ascending order.
     *
     * @param messageIdList The IDs of the messages which are to be [query]d. They must be sorted in ascending order.
     * @see searchGroupChat
     * @see searchPrivateChat
     */
    private fun search(
        messageIdList: LinkedHashSet<Int>,
        query: String,
        pagination: BackwardPagination? = null,
    ): LinkedHashSet<Int> = messageIdList
        .let { list ->
            if (pagination?.before == null) list else list.takeWhile { it < pagination.before }
        }
        .filter { messageId ->
            when (readType(messageId)) {
                MessageType.TEXT -> TextMessages.read(messageId).value.contains(query, ignoreCase = true)
                MessageType.ACTION -> {
                    ActionMessages.readText(messageId).value.contains(query, ignoreCase = true) ||
                            ActionMessageActions.read(messageId).any { it.value.contains(query, ignoreCase = true) }
                }
                MessageType.IMAGE ->
                    ImageMessages.readCaption(messageId)?.value?.contains(query, ignoreCase = true) ?: false
                MessageType.POLL -> {
                    PollMessages.readQuestion(messageId).value.contains(query, ignoreCase = true) ||
                            PollMessageOptions.readOptions(messageId).any {
                                it.value.contains(query, ignoreCase = true)
                            }
                }
                MessageType.AUDIO, MessageType.GROUP_CHAT_INVITE, MessageType.DOC, MessageType.VIDEO -> false
            }
        }
        .let { if (pagination?.last == null) it else it.takeLast(pagination.last) }
        .toLinkedHashSet()

    /**
     * Returns the [chatId]'s message IDs (sorted in ascending order) which haven't been deleted by the [userId] such as
     * through [PrivateChatDeletions].
     */
    fun readPrivateChat(userId: Int, chatId: Int, pagination: BackwardPagination? = null): LinkedHashSet<Int> {
        val op = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        return readChat(chatId, pagination, op)
    }

    /** Returns the [chatId]'s message IDs sorted in ascending order. */
    fun readGroupChat(chatId: Int, pagination: BackwardPagination? = null): LinkedHashSet<Int> =
        readChat(chatId, pagination)

    /**
     * Returns the [chatId]'s message IDs as per the [pagination]. The returned message IDs are concrete classes
     * sorted in ascending order.
     *
     * @see readPrivateChat
     * @see readGroupChat
     */
    private fun readChat(
        chatId: Int,
        pagination: BackwardPagination? = null,
        filter: Filter = null,
    ): LinkedHashSet<Int> {
        var op = this.chatId eq chatId
        pagination?.before?.let { op = op and (Messages.id less it) }
        filter?.let { op = op and it }
        return transaction {
            select(op)
                .orderBy(Messages.id, SortOrder.DESC)
                .let { if (pagination?.last == null) it else it.limit(pagination.last) }
                .reversed()
                .map { it[Messages.id].value }
                .toLinkedHashSet()
        }
    }

    /** Returns the IDs of messages in the [chatId] sorted in ascending order. */
    fun readIdList(chatId: Int): LinkedHashSet<Int> = transaction {
        select(Messages.chatId eq chatId).orderBy(Messages.id).map { it[Messages.id].value }.toLinkedHashSet()
    }

    /** @see isValidContext */
    fun isExistingChatMessage(chatId: Int, messageId: Int): Boolean =
        transaction { select((Messages.chatId eq chatId) and (Messages.id eq messageId)).empty().not() }

    fun isForwarded(messageId: Int): Boolean = transaction { select(Messages.id eq messageId).first()[isForwarded] }

    fun readSent(messageId: Int): LocalDateTime = transaction { select(Messages.id eq messageId).first()[sent] }

    /** The ID of the chat which contains the [messageId]. */
    fun readChatId(messageId: Int): Int = transaction { select(Messages.id eq messageId).first()[chatId] }

    fun hasContext(messageId: Int): Boolean = transaction { select(Messages.id eq messageId).first()[hasContext] }

    fun readContextMessageId(messageId: Int): Int? =
        transaction { select(Messages.id eq messageId).first()[contextMessageId] }

    /** [Messages] with [contextMessageId]s of deleted messages will have their [contextMessageId] set to `null`. */
    private fun deleteMessages(messageIdList: Collection<Int>) {
        Bookmarks.deleteBookmarks(messageIdList)
        TextMessages.delete(messageIdList)
        ActionMessages.delete(messageIdList)
        ImageMessages.delete(messageIdList)
        AudioMessages.delete(messageIdList)
        PollMessages.delete(messageIdList)
        DocMessages.delete(messageIdList)
        VideoMessages.delete(messageIdList)
        GroupChatInviteMessages.deleteMessages(messageIdList)
        transaction {
            update({ contextMessageId inList messageIdList }) { it[contextMessageId] = null }
            deleteWhere { Messages.id inList messageIdList }
        }
    }

    /** Deletes every message from the [chatId]. */
    fun deleteChat(chatId: Int): Unit = deleteMessages(readMessageIdList(chatId))

    /** Deletes all messages in the [chatId] [until] the specified [LocalDateTime]. */
    fun deleteChatUntil(chatId: Int, until: LocalDateTime): Unit =
        deleteMessages(readMessageIdList(chatId, sent less until))

    /**
     * Deletes all messages the [userId] created in the [chatId].
     *
     * Subscribers will be notified of the [UserChatMessagesRemoval] via [messagesNotifier]. If the [chatId] is a public
     * group chat, subscribers will also be notified via [chatMessagesNotifier].
     */
    fun deleteUserChatMessages(chatId: Int, userId: Int) {
        deleteMessages(readMessageIdList(chatId, senderId eq userId))
        val update = UserChatMessagesRemoval(chatId, userId)
        messagesNotifier.publish(update, readUserIdList(chatId).map(::UserId))
        if (GroupChats.isExistingPublicChat(chatId)) chatMessagesNotifier.publish(update, ChatId(chatId))
    }

    /**
     * Deletes all messages the [userId] created, and notifies subscribers of the [UserChatMessagesRemoval] via
     * [messagesNotifier]. Nothing will happen if the [userId] doesn't exist.
     */
    fun deleteUserMessages(userId: Int): Unit = transaction {
        select(senderId eq userId).withDistinct().map { it[chatId] }
    }.forEach { deleteUserChatMessages(it, userId) }

    /**
     * Deletes the [messageIdList].
     *
     * Subscribers will be notified of the [DeletedMessage]s via [messagesNotifier]. If a message is from a public group
     * chat, then subscribers will be notified via [chatMessagesNotifier] as well.
     */
    fun delete(messageIdList: Collection<Int>) {
        messageIdList.forEach {
            val chatId = readChatId(it)
            val update = DeletedMessage(chatId, it)
            messagesNotifier.publish(update, readUserIdList(chatId).map(::UserId))
            if (GroupChats.isExistingPublicChat(chatId)) chatMessagesNotifier.publish(update, ChatId(chatId))
        }
        deleteMessages(messageIdList)
    }

    fun delete(vararg messageIdList: Int): Unit = delete(messageIdList.toList())

    /** Whether there are messages in the [chatId] [from] the [LocalDateTime]. */
    fun isExistingFrom(chatId: Int, from: LocalDateTime): Boolean =
        transaction { select((Messages.chatId eq chatId) and (sent greaterEq from)).empty().not() }

    /** Every message [id] in the [chatId]. */
    private fun readMessageIdList(chatId: Int, filter: Filter = null): Set<Int> = transaction {
        var op = Messages.chatId eq chatId
        filter?.let { op = op and filter }
        select(op).map { it[Messages.id].value }.toSet()
    }

    fun isExisting(messageId: Int): Boolean = transaction { select(Messages.id eq messageId).empty().not() }

    /** Returns the [type] of [Cursor] for the private [chatId] as seen by the [userId]. */
    fun readPrivateChatCursor(userId: Int, chatId: Int, type: CursorType): Cursor? {
        val filter = PrivateChatDeletions.readLastDeletion(chatId, userId)?.let { sent greater it }
        return readCursor(chatId, type, filter)
    }

    /** Returns the [type] of [Cursor] for the group [chatId]. */
    fun readGroupChatCursor(chatId: Int, type: CursorType): Cursor? = readCursor(chatId, type)

    /**
     * Returns the [type] of [Cursor] for the [chatId].
     *
     * @see readPrivateChatCursor
     * @see readGroupChatCursor
     */
    private fun readCursor(chatId: Int, type: CursorType, filter: Filter = null): Cursor? {
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
     * @see isValidContext
     */
    fun isVisible(userId: Int?, messageId: Int): Boolean {
        if (!isExisting(messageId)) return false
        val chatId = readChatId(messageId)
        if (GroupChats.isExistingPublicChat(chatId)) return true
        if (userId == null) return false
        if (!isUserInChat(userId, chatId)) return false
        if (chatId in GroupChatUsers.readChatIdList(userId)) return true
        val deletion = PrivateChatDeletions.readLastDeletion(chatId, userId) ?: return true
        return readSent(messageId) >= deletion
    }

    /**
     * Returns whether the [contextMessageId] is visible to the [userId] in the [chatId]. If the [contextMessageId] is
     * `null`, `true` will be returned regardless of the other parameters' values.
     *
     * @see isVisible
     * @see isExistingChatMessage
     */
    fun isValidContext(userId: Int, chatId: Int, contextMessageId: Int?): Boolean = contextMessageId == null ||
            (isExistingChatMessage(chatId, contextMessageId) && isVisible(userId, contextMessageId))
}
