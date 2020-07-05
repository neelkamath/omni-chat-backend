package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Broker
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.groupChatInfoBroker
import com.neelkamath.omniChat.db.transact
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

    /** @return whether [user1Id] and [user2Id] have at least one chat in common. */
    fun areInSameChat(user1Id: String, user2Id: String): Boolean =
        user1Id in readChatIdList(user2Id).flatMap { readUserIdList(it) }

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
        AccountsConnection.build(readUserCursors(groupChatId), pagination)

    /** Adds every user in the [userIdList] to the [groupChatId] if they aren't in it. */
    fun addUsers(groupChatId: Int, userIdList: List<String>): Unit = transact {
        val users = userIdList.filterNot { isUserInChat(groupChatId, it) }.toSet()
        batchInsert(users) {
            this[GroupChatUsers.groupChatId] = groupChatId
            this[userId] = it
        }
    }

    /**
     * Removes users in the [userIdList] from the [chatId], ignoring the IDs of users who aren't in the chat. If every
     * user is removed, the [chatId] will be [GroupChats.delete]d.
     *
     * If the chat is deleted, it will be deleted from [GroupChats], [GroupChatUsers], [Messages], and
     * [MessageStatuses]. Clients excluding the [userIdList] who have [Broker.subscribe]d via [groupChatInfoBroker] will
     * be [Broker.notify]d of the [ExitedUser]s.
     */
    fun removeUsers(chatId: Int, userIdList: List<String>) {
        transact {
            deleteWhere { (groupChatId eq chatId) and (userId inList userIdList) }
        }
        userIdList.forEach { userId ->
            groupChatInfoBroker.notify(ExitedUser(chatId, userId)) { isUserInChat(chatId, it.userId) }
        }
        if (readUserIdList(chatId).isEmpty()) GroupChats.delete(chatId)
    }

    /** Convenience function for [removeUsers]. */
    fun removeUsers(chatId: Int, vararg userIdList: String): Unit = removeUsers(chatId, userIdList.toList())

    /** Returns the chat ID list of every chat the [userId] is in. */
    fun readChatIdList(userId: String): List<Int> = transact {
        select { GroupChatUsers.userId eq userId }.map { it[groupChatId] }
    }
}