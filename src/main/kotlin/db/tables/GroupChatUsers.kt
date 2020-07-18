package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.AccountEdge
import com.neelkamath.omniChat.AccountsConnection
import com.neelkamath.omniChat.ExitedUser
import com.neelkamath.omniChat.db.Broker
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.updatedChatsBroker
import com.neelkamath.omniChat.readUserById
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** The users in [GroupChats]. */
object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val groupChatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)

    private fun isUserInChat(groupChatId: Int, userId: Int): Boolean = transaction {
        !select { (GroupChatUsers.groupChatId eq groupChatId) and (GroupChatUsers.userId eq userId) }.empty()
    }

    /** Returns the ID of every user the [userId] has a chat with, excluding their own ID. */
    fun readFellowParticipants(userId: Int): Set<Int> =
        readChatIdList(userId).flatMap { readUserIdList(it) }.toSet() - userId

    /**
     * The user ID list from the specified [groupChatId].
     *
     * @see [readUsers]
     */
    fun readUserIdList(groupChatId: Int): List<Int> = transaction {
        select { GroupChatUsers.groupChatId eq groupChatId }.map { it[userId] }
    }

    private fun readUserCursors(groupChatId: Int): List<AccountEdge> = transaction {
        select { GroupChatUsers.groupChatId eq groupChatId }
            .map { AccountEdge(readUserById(it[userId]), it[GroupChatUsers.id].value) }
    }

    /** @see [readUserIdList] */
    fun readUsers(groupChatId: Int, pagination: ForwardPagination? = null): AccountsConnection =
        AccountsConnection.build(readUserCursors(groupChatId), pagination)

    /** Adds every user in the [userIdList] to the [groupChatId] if they aren't in it. */
    fun addUsers(groupChatId: Int, userIdList: List<Int>): Unit = transaction {
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
     * [MessageStatuses]. Clients, excluding the [userIdList], who have [Broker.subscribe]d via [updatedChatsBroker]
     * will be [Broker.notify]d of the [ExitedUser]s.
     */
    fun removeUsers(chatId: Int, userIdList: List<Int>) {
        transaction {
            deleteWhere { (groupChatId eq chatId) and (userId inList userIdList) }
        }
        userIdList.forEach { userId ->
            updatedChatsBroker.notify(ExitedUser(chatId, userId)) { isUserInChat(chatId, it.userId) }
        }
        if (readUserIdList(chatId).isEmpty()) GroupChats.delete(chatId)
    }

    /** Convenience function for [removeUsers]. */
    fun removeUsers(chatId: Int, vararg userIdList: Int): Unit = removeUsers(chatId, userIdList.toList())

    /** Removes the [userId] from every chat they're in, [GroupChats.delete]ing the chat if they're the last user. */
    fun removeUser(userId: Int): Unit = readChatIdList(userId).forEach { removeUsers(it, userId) }

    /** The chat ID list of every chat the [userId] is in. */
    fun readChatIdList(userId: Int): List<Int> = transaction {
        select { GroupChatUsers.userId eq userId }.map { it[groupChatId] }
    }
}