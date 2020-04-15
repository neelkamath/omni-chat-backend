package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Auth
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.keycloak.representations.idm.UserRepresentation

data class PrivateChat(val id: Int, val creatorUserId: String, val invitedUserId: String)

private data class PrivateUserChat(val user: UserRepresentation, val chatId: Int)

object PrivateChats : IntIdTable() {
    override val tableName get() = "private_chats"
    private val creatorUserId = varchar("creator_user_id", Auth.userIdLength)
    private val invitedUserId = varchar("invited_user_id", Auth.userIdLength)

    /** Returns the chat ID after creating it. */
    fun create(creatorUserId: String, invitedUserId: String): Int = Db.transact {
        insertAndGetId {
            it[this.creatorUserId] = creatorUserId
            it[this.invitedUserId] = invitedUserId
        }.value
    }

    fun read(creatorUserId: String): List<PrivateChat> = Db.transact {
        select { PrivateChats.creatorUserId eq creatorUserId }
            .map { PrivateChat(it[id].value, it[this.creatorUserId], it[invitedUserId]) }
    }

    /**
     * Searches chats the user (specified by the [userId]) is in.
     *
     * The other users the specified user is chatting with have their username, first name, and last name matched with
     * the [query]. The search results are returned as a chat ID [List].
     */
    fun search(userId: String, query: String): List<Int> = Db.transact {
        select { (creatorUserId eq userId) or (invitedUserId eq userId) }
            .map {
                val user = if (it[creatorUserId] == userId) it[invitedUserId] else userId
                PrivateUserChat(Auth.findUserById(user), it[id].value)
            }
            .filter { matchesUser(it.user, query) }
            .map { it.chatId }
    }

    /**
     * Whether the [query] matches the [user] (case-insensitive).
     *
     * Checks if there's a match between the [user]'s [UserRepresentation.username], [UserRepresentation.firstName], or
     * [UserRepresentation.lastName].
     */
    private fun matchesUser(user: UserRepresentation, query: String): Boolean = with(user) {
        containsQuery(username, query) || containsQuery(firstName, query) || containsQuery(lastName, query)
    }

    /** Whether the [string] contains the [query] (case-insensitive). */
    private fun containsQuery(string: String?, query: String): Boolean =
        string != null && query.toLowerCase() in string.toLowerCase()
}