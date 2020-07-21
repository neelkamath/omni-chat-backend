package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.GroupChats.MAX_PIC_BYTES
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Pics cannot exceed [MAX_PIC_BYTES].
 *
 * @see [GroupChatUsers]
 * @see [Messages]
 * @see [TypingStatuses]
 * @see [Chats]
 */
object GroupChats : Table() {
    override val tableName get() = "group_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
    private val adminId: Column<Int> = integer("admin_id").references(Users.id)

    /** Titles cannot exceed this length. */
    const val MAX_TITLE_LENGTH = 70

    private val title: Column<String> = varchar("title", MAX_TITLE_LENGTH)

    /** Descriptions cannot exceed this length. */
    const val MAX_DESCRIPTION_LENGTH = 1000

    private val description: Column<String> = varchar("description", MAX_DESCRIPTION_LENGTH)

    /** The pic cannot exceed 100 KiB. */
    const val MAX_PIC_BYTES = 100 * 1024

    private val pic: Column<ByteArray?> = binary("pic", MAX_PIC_BYTES).nullable()

    /** Whether the [userId] is the admin of [chatId] (assumed to exist). */
    fun isAdmin(userId: Int, chatId: Int): Boolean = transaction {
        select { GroupChats.id eq chatId }.first()[adminId] == userId
    }

    /**
     * Sets the [userId] as the admin of the [chatId]. A
     *
     * @throws [IllegalArgumentException] if the [userId] isn't in the chat.
     */
    fun setAdmin(chatId: Int, userId: Int) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userId !in userIdList)
            throw IllegalArgumentException("The new admin (ID: $userId) isn't in the chat (users: $userIdList).")
        transaction {
            update({ GroupChats.id eq chatId }) { it[adminId] = userId }
        }
    }

    /**
     * Returns the [chat]'s ID after creating it.
     *
     * [Broker.notify]s the [GroupChatInput.userIdList], excluding the admin, of the [GroupChatInput] via
     * [newGroupChatsBroker].
     */
    fun create(adminId: Int, chat: GroupChatInput): Int {
        val chatId = transaction {
            insert {
                it[id] = Chats.create()
                it[this.adminId] = adminId
                it[title] = chat.title.value
                it[description] = chat.description.value
            }[GroupChats.id]
        }
        GroupChatUsers.addUsers(chatId, chat.userIdList + adminId)
        newGroupChatsBroker.notify(GroupChatId(chatId)) { it.userId in chat.userIdList - adminId }
        return chatId
    }

    /**
     * Returns the [chatId] for the [userId].
     *
     * @param[messagesPagination] pagination for [GroupChat.messages].
     */
    fun readChat(
        userId: Int,
        chatId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): GroupChat {
        val row = transaction {
            select { GroupChats.id eq chatId }.first()
        }
        return buildGroupChat(row, userId, chatId, usersPagination, messagesPagination)
    }

    /**
     * @param[usersPagination] pagination for [GroupChat.users].
     * @param[messagesPagination] pagination for [GroupChat.messages].
     * @return the [userId]'s chats.
     * @see [GroupChatUsers.readChatIdList]
     */
    fun readUserChats(
        userId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transaction {
        GroupChatUsers.readChatIdList(userId).map { readChat(userId, it, usersPagination, messagesPagination) }
    }

    /**
     * [update]s the chat.
     *
     * Users in the [GroupChatUpdate.newUserIdList] who are already in the chat are ignored.
     *
     * Users in the [GroupChatUpdate.removedUserIdList] who aren't in the chat are ignored. Removed users will be
     * [Broker.unsubscribe]d via [updatedChatsBroker]. The chat is deleted if every user is removed.
     *
     * A [UpdatedGroupChat] is sent to clients who have [Broker.subscribe]d via [updatedChatsBroker]. Clients who have
     * [Broker.subscribe]d via [newGroupChatsBroker] will be [Broker.notify]d of the [GroupChat].
     *
     * @see [GroupChatUsers.addUsers]
     * @see [GroupChatUsers.removeUsers]
     * @see [updateTitleAndDescription]
     * @see [updatePic]
     */
    fun update(update: GroupChatUpdate) {
        val existingUserIdList = GroupChatUsers.readUserIdList(update.chatId)
        val removedUserIdList =
            if (update.removedUserIdList == null) null
            else update.removedUserIdList.intersect(existingUserIdList).toList()
        if (removedUserIdList != null) {
            GroupChatUsers.removeUsers(update.chatId, removedUserIdList)
            if (removedUserIdList.containsAll(existingUserIdList)) return
        }
        updateTitleAndDescription(update)
        with(update) { if (newAdminId != null) setAdmin(chatId, newAdminId) }
        val newUserIdList = if (update.newUserIdList == null) null else update.newUserIdList - existingUserIdList
        if (newUserIdList != null) {
            GroupChatUsers.addUsers(update.chatId, newUserIdList)
            newGroupChatsBroker.notify(GroupChatId(update.chatId)) { it.userId in newUserIdList }
        }
        val chat =
            update.copy(newUserIdList = newUserIdList, removedUserIdList = removedUserIdList).toUpdatedGroupChat()
        updatedChatsBroker.notify(chat) { isUserInChat(it.userId, update.chatId) }
    }

    /**
     * [update]s the [GroupChatUpdate.chatId]'s title and description if they aren't `null`.
     *
     * @see [GroupChats.update]
     */
    private fun updateTitleAndDescription(update: GroupChatUpdate): Unit = transaction {
        update({ GroupChats.id eq update.chatId }) { statement ->
            update.title?.let { statement[title] = it.value }
            update.description?.let { statement[description] = it.value }
        }
    }

    /**
     * Deletes the [pic] if it's `null`. [Broker.subscribe]rs will be [Broker.notify]d of the [UpdatedGroupChat].
     *
     * @see [update]
     */
    fun updatePic(chatId: Int, pic: ByteArray?) {
        transaction {
            update({ GroupChats.id eq chatId }) { it[this.pic] = pic }
        }
        updatedChatsBroker.notify(UpdatedGroupChat(chatId)) { isUserInChat(it.userId, chatId) }
    }

    fun readPic(chatId: Int): ByteArray? = transaction {
        select { GroupChats.id eq chatId }.first()[pic]
    }

    /**
     * Deletes the [chatId] from [Chats], [GroupChats], [TypingStatuses], [Messages], and [MessageStatuses]. Clients who
     * have [Broker.subscribe]d to [MessagesSubscription]s via [messagesBroker] will receive a [DeletionOfEveryMessage].
     *
     * @throws [IllegalArgumentException] if the [chatId] has users in it.
     */
    fun delete(chatId: Int) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userIdList.isNotEmpty())
            throw IllegalArgumentException("The chat (ID: $chatId) is not empty (users: $userIdList).")
        TypingStatuses.deleteChat(chatId)
        Messages.deleteChat(chatId)
        transaction {
            deleteWhere { GroupChats.id eq chatId }
        }
        Chats.delete(chatId)
    }

    /**
     * @param[usersPagination] pagination for [GroupChat.messages].
     * @param[messagesPagination] pagination for [GroupChat.messages].
     * @return chats after case-insensitively [query]ing the title of every chat the [userId] is in.
     */
    fun search(
        userId: Int,
        query: String,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): List<GroupChat> = transaction {
        select { (GroupChats.id inList GroupChatUsers.readChatIdList(userId)) and (title iLike query) }
            .map { buildGroupChat(it, userId, it[GroupChats.id], usersPagination, messagesPagination) }
    }

    /**
     * Builds the [chatId] from the [row] for the [userId].
     *
     * @param[messagesPagination] pagination for [GroupChat.messages].
     * @param[usersPagination] pagination for [GroupChat.users].
     */
    private fun buildGroupChat(
        row: ResultRow,
        userId: Int,
        chatId: Int,
        usersPagination: ForwardPagination? = null,
        messagesPagination: BackwardPagination? = null
    ): GroupChat = GroupChat(
        chatId,
        row[adminId],
        GroupChatUsers.readUsers(chatId, usersPagination),
        GroupChatTitle(row[title]),
        GroupChatDescription(row[description]),
        Messages.readGroupChatConnection(userId, chatId, messagesPagination)
    )

    /** Whether the [userId] is the admin of a group chat containing members other than themselves. */
    fun isNonemptyChatAdmin(userId: Int): Boolean = readUserChats(
        userId,
        usersPagination = ForwardPagination(first = 2),
        messagesPagination = BackwardPagination(last = 0)
    ).filter { it.users.edges.size > 1 }.map { it.adminId }.contains(userId)

    /**
     * Case-insensitively [query]s the messages in the chats the [userId] is in. Only chats having messages matching the
     * [query] will be returned. Only the matched message [ChatEdges.edges] will be returned.
     */
    fun queryUserChatEdges(userId: Int, query: String): List<ChatEdges> = GroupChatUsers.readChatIdList(userId)
        .associateWith { Messages.searchGroupChat(userId, it, query) }
        .filter { (_, edges) -> edges.isNotEmpty() }
        .map { (chatId, edges) -> ChatEdges(chatId, edges) }
}