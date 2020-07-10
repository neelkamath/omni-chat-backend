package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

/**
 * The ID of every user in the auth system. The ID is useful because the auth system doesn't give us unique integer IDs,
 * which are required to correctly paginate.
 */
object Users : IntIdTable() {
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)

    /** @see [createUser] */
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
        return AccountsConnection.build(users, pagination)
    }

    /**
     * Deletes the [userId] from this table.
     *
     * @see [deleteUser]
     */
    fun delete(userId: String): Unit = transact {
        deleteWhere { Users.userId eq userId }
    }
}