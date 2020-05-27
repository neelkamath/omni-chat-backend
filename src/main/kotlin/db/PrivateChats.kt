package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.keycloak.representations.idm.UserRepresentation

/**
 * [PrivateChatDeletions] stores when each user deleted the chat. [Messages] holds chat messages. If both the users have
 * deleted the chat, then the chat's record will be deleted.
 */
object PrivateChats : IntIdTable() {
    override val tableName get() = "private_chats"
    private val user1Id: Column<String> = varchar("user_1_id", USER_ID_LENGTH)
    private val user2Id: Column<String> = varchar("user_2_id", USER_ID_LENGTH)

    /** Returns the created chat's ID. An [IllegalArgumentException] will be thrown if the chat exists. */
    fun create(user1Id: String, user2Id: String): Int {
        if (exists(user1Id, user2Id))
            throw IllegalArgumentException("The chat between user 1 (ID: $user1Id) and user 2 (ID: $user2Id) exists.")
        return insert(user1Id, user2Id)
    }

    /** Records in the DB that [user1Id] and [user2Id] are in a chat with each other, and returns the chat's ID. */
    private fun insert(user1Id: String, user2Id: String): Int = transact {
        insertAndGetId {
            it[this.user1Id] = user1Id
            it[this.user2Id] = user2Id
        }.value
    }

    /**
     * Returns the [userId]'s chats. Chats the [userId] deleted, which had no activity after their deletion, are not
     * returned.
     *
     * If you just need the chat IDs, [readIdList] is more efficient.
     */
    fun read(userId: String): List<PrivateChat> = readRows(userId).map { buildPrivateChat(it, userId) }

    fun read(id: Int, userId: String): PrivateChat = transact {
        select { PrivateChats.id eq id }.first()
    }.let { buildPrivateChat(it, userId) }

    private fun buildPrivateChat(row: ResultRow, userId: String): PrivateChat {
        val chatId = row[id].value
        val otherUserId = if (row[user1Id] == userId) row[user2Id] else row[user1Id]
        return PrivateChat(chatId, findUserById(otherUserId), Messages.read(chatId, userId))
    }

    /**
     * Returns the ID list of the [userId]'s chats. Chats the [userId] deleted, which had no activity after their
     * deletion, are not returned.
     */
    fun readIdList(userId: String): List<Int> = readRows(userId).map { it[id].value }

    /**
     * Returns every chat's [ResultRow] the [userId] has. Chats the user deleted, which had no activity after their
     * deletion, are not returned.
     */
    private fun readRows(userId: String): List<ResultRow> = transact {
        select { (user1Id eq userId) or (user2Id eq userId) }
            .filterNot { PrivateChatDeletions.isDeleted(userId, it[PrivateChats.id].value) }
    }

    /** Whether [user1Id] and [user2Id] are in a chat with each other. */
    fun exists(user1Id: String, user2Id: String): Boolean {
        val where = { userId: String -> (PrivateChats.user1Id eq userId) or (PrivateChats.user2Id eq userId) }
        return transact {
            !select { where(user1Id) and where(user2Id) }.empty()
        }
    }

    /**
     * Searches chats the [userId] has by case-insensitively [query]ing other users' first name, last name, and
     * username. Chats the [userId] deleted, which had no activity after their deletion, are not searched.
     */
    fun search(userId: String, query: String): List<PrivateChat> =
        read(userId).filter { findUserById(it.user.id).matches(query) }

    /**
     * Deletes every chat the [userId] is in from [PrivateChats], [PrivateChatDeletions], [Messages], and
     * [MessageStatuses]. Chats the [userId] deleted, which had no activity after their deletion, are deleted as well.
     * Clients will be notified of a [DeletionOfEveryMessage], and then [unsubscribeFromMessageUpdates].
     */
    fun delete(userId: String) {
        val chatIdList = transact {
            select { (user1Id eq userId) or (user2Id eq userId) }.map { it[PrivateChats.id].value }
        }
        chatIdList.forEach {
            PrivateChatDeletions.delete(it)
            Messages.deleteChat(it)
        }
        transact {
            deleteWhere { PrivateChats.id inList chatIdList }
        }
    }

    /** Deletes the [chatId] from [PrivateChats]. */
    fun delete(chatId: Int) {
        transact {
            deleteWhere { PrivateChats.id eq chatId }
        }
    }

    /** Returns the IDs of the users in the [chatId]. */
    fun readUsers(chatId: Int): List<String> = transact {
        val row = select { PrivateChats.id eq chatId }.first()
        listOf(row[user1Id], row[user2Id])
    }

    /**
     * Checks if this user's [UserRepresentation.username], [UserRepresentation.firstName], or
     * [UserRepresentation.lastName] case-insensitively match the [query].
     */
    private fun AccountInfo.matches(query: String): Boolean = containsQuery(username, query)
            || containsQuery(emailAddress, query)
            || containsQuery(firstName, query)
            || containsQuery(lastName, query)

    /** Whether the [string] contains the [query] (case-insensitive). */
    private fun containsQuery(string: String?, query: String): Boolean =
        string != null && query.toLowerCase() in string.toLowerCase()
}