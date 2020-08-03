package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.Broker
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.newGroupChatsBroker
import com.neelkamath.omniChat.db.updatedChatsBroker
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.readUserById
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/** The users in [GroupChats]. */
object GroupChatUsers : IntIdTable() {
    override val tableName get() = "group_chat_users"
    private val chatId: Column<Int> = integer("group_chat_id").references(GroupChats.id)
    private val userId: Column<Int> = integer("user_id").references(Users.id)
    private val isAdmin: Column<Boolean> = bool("is_admin")

    private fun isUserInChat(userId: Int, chatId: Int): Boolean = transaction {
        !select { (GroupChatUsers.chatId eq chatId) and (GroupChatUsers.userId eq userId) }.empty()
    }

    /**
     * Makes the [userIdList] admins of the [chatId]. An [IllegalArgumentException] will be thrown if the a user isn't
     * in the chat.
     *
     * [Broker.notify]s [Broker.subscribe]rs of the [UpdatedGroupChat] via [updatedChatsBroker].
     */
    fun makeAdmins(chatId: Int, userIdList: List<Int>) {
        val invalidUsers = userIdList.filterNot { isUserInChat(it, chatId) }
        if (invalidUsers.isNotEmpty()) throw IllegalArgumentException("$invalidUsers aren't in the chat (ID: $chatId).")
        transaction {
            update({ (GroupChatUsers.chatId eq chatId) and (userId inList userIdList) }) { it[isAdmin] = true }
        }
        val update = UpdatedGroupChat(chatId, adminIdList = readAdminIdList(chatId))
        updatedChatsBroker.notify(update) { isUserInChat(it.userId, chatId) }
    }

    /** Returns the ID of every user the [userId] has a chat with, excluding their own ID. */
    fun readFellowParticipants(userId: Int): Set<Int> =
        readChatIdList(userId).flatMap { readUserIdList(it) }.toSet() - userId

    /** Whether the [userId] is an admin of the [chatId] (assumed to exist). */
    fun isAdmin(userId: Int, chatId: Int): Boolean = transaction {
        select { GroupChatUsers.chatId eq chatId }.any { it[GroupChatUsers.userId] == userId && it[isAdmin] }
    }

    /**
     * The user ID list from the specified [chatId].
     *
     * @see [readUsers]
     * @see [readAdminIdList]
     */
    fun readUserIdList(chatId: Int): List<Int> = transaction {
        select { GroupChatUsers.chatId eq chatId }.map { it[userId] }
    }

    fun readAdminIdList(chatId: Int): List<Int> = transaction {
        select { (GroupChatUsers.chatId eq chatId) and (isAdmin eq true) }.map { it[userId] }
    }

    private fun readUserCursors(chatId: Int): List<AccountEdge> = transaction {
        select { GroupChatUsers.chatId eq chatId }
            .map { AccountEdge(readUserById(it[userId]), it[GroupChatUsers.id].value) }
    }

    /** @see [readUserIdList] */
    fun readUsers(chatId: Int, pagination: ForwardPagination? = null): AccountsConnection =
        AccountsConnection.build(readUserCursors(chatId), pagination)

    /**
     * Adds the [users] who aren't already in the [chatId]. [Broker.notify]s [Broker.subscribe]rs of the
     * [UpdatedGroupChat] via [updatedChatsBroker], and the [GroupChatId] via [newGroupChatsBroker].
     */
    fun addUsers(chatId: Int, users: List<Int>) {
        val newUserIdList = users.filterNot { isUserInChat(it, chatId) }.toSet()
        transaction {
            batchInsert(newUserIdList) {
                this[GroupChatUsers.chatId] = chatId
                this[userId] = it
                this[isAdmin] = false
            }
        }
        newGroupChatsBroker.notify(GroupChatId(chatId)) { it.userId in newUserIdList }
        val update = UpdatedGroupChat(chatId, newUsers = newUserIdList.map(::readUserById))
        updatedChatsBroker.notify(update) { it.userId !in newUserIdList && isUserInChat(it.userId, chatId) }
    }

    /**
     * Whether the [userIdList] can be removed from the [chatId]. Returns `false` if there would be users sans admins
     * left in the [chatId], or if the [userIdList] contains users who aren't in the [chatId].
     */
    fun canUsersLeave(chatId: Int, userIdList: List<Int>): Boolean = canUsersLeave(chatId, userIdList.toSet())

    private fun canUsersLeave(chatId: Int, userIdList: Set<Int>): Boolean {
        val existingUserIdList = readUserIdList(chatId).toSet()
        if (userIdList.any { it !in existingUserIdList }) return false
        return userIdList == existingUserIdList || (existingUserIdList - userIdList).any { isAdmin(it, chatId) }
    }

    private fun canUsersLeave(chatId: Int, vararg userIdList: Int): Boolean = canUsersLeave(chatId, userIdList.toSet())

    /**
     * Removes users in the [userIdList] from the [chatId]. An [IllegalArgumentException] will be thrown if not
     * [canUsersLeave]. If every user is removed, the [chatId] will be [GroupChats.delete]d. Returns whether the chat
     * was deleted.
     *
     * [Broker.subscribe]rs, excluding the [userIdList], will be [Broker.notify]d of the [ExitedUser]s via the
     * [updatedChatsBroker].
     */
    fun removeUsers(chatId: Int, userIdList: List<Int>): Boolean {
        if (!canUsersLeave(chatId, userIdList))
            throw IllegalArgumentException("The users ($userIdList) cannot leave because the chat needs an admin.")
        transaction {
            deleteWhere { (GroupChatUsers.chatId eq chatId) and (userId inList userIdList) }
        }
        for (userId in userIdList.toSet())
            updatedChatsBroker.notify(ExitedUser(userId, chatId)) { isUserInChat(it.userId, chatId) }
        if (readUserIdList(chatId).isEmpty()) {
            GroupChats.delete(chatId)
            return true
        }
        return false
    }

    fun removeUsers(chatId: Int, vararg userIdList: Int): Boolean = removeUsers(chatId, userIdList.toList())

    /**
     * Whether the [userId] can leave every chat they're in. Returns `false` only if they're the last admin of a chat
     * with other users in it.
     */
    fun canUserLeave(userId: Int): Boolean = readChatIdList(userId).all { canUsersLeave(it, userId) }

    /** Calls [removeUsers] on the [userId] for every chat they're in. */
    fun removeUser(userId: Int): Unit = readChatIdList(userId).forEach { removeUsers(it, userId) }

    /** The chat ID list of every chat the [userId] is in. */
    fun readChatIdList(userId: Int): List<Int> = transaction {
        select { GroupChatUsers.userId eq userId }.map { it[chatId] }
    }
}