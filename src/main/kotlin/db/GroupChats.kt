package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import org.jetbrains.exposed.sql.*

/** The [GroupChatUsers] table contains the participants. [GroupChats] have [Messages]. */
object GroupChats : Table() {
    override val tableName get() = "group_chats"
    val id: Column<Int> = integer("id").uniqueIndex().references(Chats.id)
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
        val userIdList = GroupChatUsers.readUserIdList(chatId)
        if (userId !in userIdList)
            throw IllegalArgumentException("The new admin (ID: $userId) isn't in the chat (users: $userIdList).")
        transact {
            update({ GroupChats.id eq chatId }) { it[adminId] = userId }
        }
    }

    /** Returns the [chat]'s ID after creating it. */
    fun create(adminId: String, chat: NewGroupChat): Int {
        val chatId = transact {
            insert {
                it[id] = Chats.create()
                it[this.adminId] = adminId
                it[title] = chat.title
                it[description] = chat.description
            }[GroupChats.id]
        }
        GroupChatUsers.addUsers(chatId, chat.userIdList + adminId)
        return chatId
    }

    fun read(chatId: Int, pagination: BackwardPagination? = null): GroupChat = transact {
        select { GroupChats.id eq chatId }.first()
    }.let { buildGroupChat(it, chatId, pagination) }

    /**
     * Returns the [userId]'s chats.
     *
     * @see [GroupChatUsers.readChatIdList]
     */
    fun read(userId: String, pagination: BackwardPagination? = null): List<GroupChat> = transact {
        GroupChatUsers.readChatIdList(userId).map { read(it, pagination) }
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

    /** Returns chats after case-insensitively [query]ing the title of every chat the [userId] is in. */
    fun search(userId: String, query: String, pagination: BackwardPagination? = null): List<GroupChat> = transact {
        select { (GroupChats.id inList GroupChatUsers.readChatIdList(userId)) and (title iLike query) }
            .map { buildGroupChat(it, it[GroupChats.id], pagination) }
    }

    /** Builds the [chatId] from the [row]. */
    private fun buildGroupChat(row: ResultRow, chatId: Int, pagination: BackwardPagination? = null): GroupChat =
        GroupChat(
            chatId,
            row[adminId],
            GroupChatUsers.readUserIdList(chatId).map(::findUserById).toSet(),
            row[title],
            row[description],
            Messages.buildGroupChatConnection(chatId, pagination)
        )

    /** Whether the [userId] is the admin of a group chat containing members other than themselves. */
    fun isNonemptyChatAdmin(userId: String): Boolean =
        userId in read(userId, BackwardPagination(last = 0)).filter { it.users.size > 1 }.map { it.adminId }
}