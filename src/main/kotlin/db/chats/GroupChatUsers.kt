package com.neelkamath.omniChat.db.chats

import com.neelkamath.omniChat.AccountEdge
import com.neelkamath.omniChat.AccountsConnection
import com.neelkamath.omniChat.USER_ID_LENGTH
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.buildAccountsConnection
import com.neelkamath.omniChat.db.messages.MessageStatuses
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.db.messages.unsubscribeUserFromMessageUpdates
import com.neelkamath.omniChat.db.transact
import com.neelkamath.omniChat.readUserById
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

/** The users in [GroupChats]. */
object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)

    private fun isUserInChat(groupChatId: Int, userId: String): Boolean = transact {
        !select { (GroupChatUsers.groupChatId eq groupChatId) and (GroupChatUsers.userId eq userId) }.empty()
    }

    /**
     * Returns the user ID list from the specified [groupChatId].
     *
     * @see [readUsers]
     */
    fun readUserIdList(groupChatId: Int): List<String> = transact {
        select { GroupChatUsers.groupChatId eq groupChatId }.map { it[userId] }
    }

    private fun readUserCursors(groupChatId: Int): List<AccountEdge> = transact {
        select { GroupChatUsers.groupChatId eq groupChatId }
            .map { AccountEdge(readUserById(it[userId]), it[GroupChatUsers.id].value) }
    }

    /** @see [readUserIdList] */
    fun readUsers(groupChatId: Int, pagination: ForwardPagination? = null): AccountsConnection =
        buildAccountsConnection(readUserCursors(groupChatId), pagination)

    /** Adds every user in the [userIdList] to the [groupChatId] if they aren't in it. */
    fun addUsers(groupChatId: Int, userIdList: List<String>): Unit = transact {
        batchInsert(userIdList.filterNot { isUserInChat(groupChatId, it) }.toSet()) {
            this[GroupChatUsers.groupChatId] = groupChatId
            this[userId] = it
        }
    }

    /**
     * Removes users in the [userIdList] from the [chatId], ignoring the IDs of users who aren't in the chat. If every
     * user is removed, the [chatId] will be [GroupChats.delete]d.
     *
     * If the chat is deleted, it will be deleted from [GroupChats], [GroupChatUsers], [Messages], and
     * [MessageStatuses]. Users will be [unsubscribeUserFromMessageUpdates]d.
     */
    fun removeUsers(chatId: Int, userIdList: List<String>) {
        transact {
            deleteWhere { (groupChatId eq chatId) and (userId inList userIdList) }
        }
        userIdList.forEach { unsubscribeUserFromMessageUpdates(it, chatId) }
        if (readUserIdList(chatId).isEmpty()) GroupChats.delete(chatId)
    }

    /** Returns the chat ID list of every chat the [userId] is in. */
    fun readChatIdList(userId: String): List<Int> = transact {
        select { GroupChatUsers.userId eq userId }.map { it[groupChatId] }
    }
}