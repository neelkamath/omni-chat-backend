package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

/**
 * The ID of every user in the auth system. This is useful because the auth system doesn't give us unique integer IDs,
 * which are required to correctly paginate.
 */
object Users : IntIdTable() {
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)

    fun create(userId: String): Unit = transact {
        insert { it[this.userId] = userId }
    }

    private fun readPrimaryKey(userId: String): Int = transact {
        select { Users.userId eq userId }.first()[Users.id].value
    }

    /**
     * Case-insensitively [query]s every user's username, first name, last name, and email address.
     *
     * @see [searchUsers]
     */
    fun search(query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val users = searchUsers(query).map { AccountEdge(it, readPrimaryKey(it.id)) }
        return buildAccountsConnection(users, pagination)
    }

    /**
     * Deletes the [userId] from this table.
     *
     * @see [deleteUserFromAuth]
     */
    fun delete(userId: String): Unit = transact {
        deleteWhere { Users.userId eq userId }
    }
}