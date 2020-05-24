package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChats.adminId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

/**
 * The [GroupChatUsers] table contains the participants, including the [adminId], of a particular chat. [Messages] holds
 * the messages.
 */
object GroupChats : IntIdTable() {
    override val tableName get() = "group_chats"
    private val adminId: Column<String> = varchar("admin_id", USER_ID_LENGTH)

    /** Titles cannot exceed this length. */
    const val MAX_TITLE_LENGTH = 70

    /** Can have at most [MAX_TITLE_LENGTH]. */
    private val title: Column<String> = varchar("title", MAX_TITLE_LENGTH)

    /** Descriptions cannot exceed this length. */
    const val MAX_DESCRIPTION_LENGTH = 1000

    /** Can have at most [MAX_DESCRIPTION_LENGTH]. */
    private val description: Column<String?> = varchar("description", MAX_DESCRIPTION_LENGTH).nullable()

    /** Whether the [userId] is the admin of [chatId] (assumed to exist). */
    fun isAdmin(userId: String, chatId: Int): Boolean = transact {
        select { GroupChats.id eq chatId }.first()[adminId] == userId
    }

    /**
     * Sets the [userId] as the admin of the [chatId]. An [IllegalArgumentException] will be thrown if the [userId]
     * isn't in the chat.
     */
    fun setAdmin(chatId: Int, userId: String) {
        val userIdList = read(chatId).users.map { it.id }
        if (userId !in userIdList)
            throw IllegalArgumentException("The new admin (ID: $userId) isn't in the chat (users: $userIdList).")
        transact {
            update({ GroupChats.id eq chatId }) { it[adminId] = userId }
        }
    }

    /** Returns the [chat]'s ID after creating it. */
    fun create(adminId: String, chat: NewGroupChat): Int {
        val chatId = transact {
            insertAndGetId {
                it[this.adminId] = adminId
                it[title] = chat.title
                it[description] = chat.description
            }.value
        }
        GroupChatUsers.addUsers(chatId, chat.userIdList + adminId)
        return chatId
    }

    fun read(chatId: Int): GroupChat {
        val row = transact {
            select { GroupChats.id eq chatId }.first()
        }
        return buildGroupChat(row, chatId)
    }

    /**
     * Returns the [userId]'s chats. If you just need the chat IDs, [GroupChatUsers.readChatIdList] is more efficient.
     */
    fun read(userId: String): List<GroupChat> = transact {
        GroupChatUsers.readChatIdList(userId).map { read(it) }
    }

    /**
     * [update]s the chat.
     *
     * Users in the [GroupChatUpdate.newUserIdList] who are already in the chat are ignored.
     *
     * Users in the [GroupChatUpdate.removedUserIdList] who aren't in the chat are ignored. Removed users will be
     * [unsubscribeFromMessageUpdates]. The chat is deleted if every user is removed.
     *
     * An [IllegalArgumentException] will be thrown if the [GroupChatUpdate.title] is empty.
     */
    fun update(update: GroupChatUpdate) {
        if (update.title != null && update.title.trim().isEmpty())
            throw IllegalArgumentException("""The title ("${update.title}") is empty.""")
        transact {
            update({ GroupChats.id eq update.chatId }) { statement ->
                update.title?.let { statement[title] = it }
                update.description?.let { statement[description] = it }
            }
        }
        update.newUserIdList.let { GroupChatUsers.addUsers(update.chatId, it) }
        update.removedUserIdList.let { GroupChatUsers.removeUsers(update.chatId, it) }
        update.newAdminId?.let { setAdmin(update.chatId, update.newAdminId) }
    }

    /**
     * Deletes the [chatId] from [GroupChats]. [Messages], and [MessageStatuses]. Users who have
     * [subscribeToMessageUpdates]rs will be notified of a [DeletionOfEveryMessage], and then
     * [unsubscribeFromMessageUpdates].
     *
     * An [IllegalArgumentException] will be thrown if the [chatId] has users in it.
     */
    fun delete(chatId: Int) {
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userIdList.isNotEmpty())
            throw IllegalArgumentException("The chat (ID: $chatId) is not empty (users: $userIdList).")
        transact {
            deleteWhere { GroupChats.id eq chatId }
        }
        Messages.deleteChat(chatId)
        unsubscribeFromMessageUpdates(chatId)
    }

    /**
     * Searches the chats the [userId] is in. Returns the chat ID list by searching for the [query] in every chat's
     * title case-insensitively.
     */
    fun search(userId: String, query: String): List<GroupChat> {
        val chatIdList = GroupChatUsers.readChatIdList(userId)
        return transact {
            select { GroupChats.id inList chatIdList }
                .filter { it[title].contains(query, ignoreCase = true) }
                .map { buildGroupChat(it, it[GroupChats.id].value) }
        }
    }

    private fun buildGroupChat(row: ResultRow, chatId: Int): GroupChat {
        val users = GroupChatUsers.readUserIdList(chatId).map(::findUserById).toSet()
        return GroupChat(chatId, row[adminId], users, row[title], row[description], Messages.readChat(chatId))
    }

    /** Whether the [userId] is the admin of a group chat containing members other than themselves. */
    fun isNonemptyChatAdmin(userId: String): Boolean =
        userId in read(userId).filter { read(it.id).users.size > 1 }.map { it.adminId }
}