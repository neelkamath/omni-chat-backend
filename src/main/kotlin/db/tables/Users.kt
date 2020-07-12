package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.negotiateUserUpdate
import com.neelkamath.omniChat.db.tables.Users.id
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*

/**
 * Stores info which would be impractical to store in Keycloak's custom user attributes. The [id] can be used for
 * pagination.
 */
object Users : IntIdTable() {
    private val userId: Column<String> = varchar("user_id", USER_ID_LENGTH)

    /** Profile pics cannot exceed 100 KiB. */
    const val MAX_PROFILE_PIC_BYTES = 100 * 1024

    private val profilePic: Column<ByteArray?> = binary("profile_pic", MAX_PROFILE_PIC_BYTES).nullable()

    /** @see [createUser] */
    fun create(userId: String, profilePic: ByteArray? = null): Unit = transact {
        insert {
            it[this.userId] = userId
            it[this.profilePic] = profilePic
        }
    }

    /** Calls [negotiateUserUpdate]. */
    fun updateProfilePic(userId: String, profilePic: ByteArray) {
        transact {
            update({ Users.userId eq userId }) { it[Users.profilePic] = profilePic }
        }
        negotiateUserUpdate(userId)
    }

    fun readProfilePic(userId: String): ByteArray? = transact {
        select { Users.userId eq userId }.first()[profilePic]
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

    /**
     * If the [userId] has a profile pic, it'll be deleted, and [negotiateUserUpdate] will be called. Otherwise, nothing
     * will happen.
     */
    fun deleteProfilePic(userId: String) {
        transact {
            update({ Users.userId eq userId }) { it[profilePic] = null }
        }
        negotiateUserUpdate(userId)
    }
}