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
 * The ID and custom fields of every user in the auth system. The ID is useful because the auth system doesn't give us
 * unique integer IDs, which are required to correctly paginate.
 */
object Users : IntIdTable() {
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)

    /** The bio cannot exceed this many characters. */
    const val MAX_BIO_LENGTH = 250

    /** At most [MAX_BIO_LENGTH] characters. */
    private val bio: Column<String> = varchar("bio", MAX_BIO_LENGTH)

    fun create(userId: String, bio: Bio): Unit = transact {
        insert {
            it[this.userId] = userId
            it[this.bio] = bio.value
        }
    }

    fun readBio(userId: String): Bio = transact {
        select { Users.userId eq userId }.first()[bio].let(::Bio)
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
     * @see [deleteUserFromAuth]
     */
    fun delete(userId: String): Unit = transact {
        deleteWhere { Users.userId eq userId }
    }
}