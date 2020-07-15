package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.negotiateUserUpdate
import com.neelkamath.omniChat.db.tables.Users.MAX_PIC_BYTES
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import java.util.*

/**
 * Stores info which would be impractical to store in Keycloak's custom user attributes. Pics cannot exceed
 * [MAX_PIC_BYTES].
 */
object Users : IntIdTable() {
    /** The ID given to the user by Keycloak. */
    private val uuid: Column<UUID> = uuid("uuid")

    /** Pics cannot exceed 100 KiB. */
    const val MAX_PIC_BYTES = 100 * 1024

    private val pic: Column<ByteArray?> = binary("pic", MAX_PIC_BYTES).nullable()

    /** @see [createUser] */
    fun create(userUuid: UUID, pic: ByteArray? = null): Unit = transact {
        insert {
            it[uuid] = userUuid
            it[this.pic] = pic
        }
    }

    fun readUuid(id: Int): UUID = transact {
        select { Users.id eq id }.first()[uuid]
    }

    fun readId(userUuid: UUID): Int = transact {
        select { uuid eq userUuid }.first()[Users.id].value
    }

    fun exists(id: Int): Boolean = transact {
        select { Users.id eq id }.empty().not()
    }

    /** Deletes the pic if [pic] is `null`. Calls [negotiateUserUpdate]. */
    fun updatePic(userId: Int, pic: ByteArray?) {
        transact {
            update({ Users.id eq userId }) { it[Users.pic] = pic }
        }
        negotiateUserUpdate(userId)
    }

    fun readPic(userId: Int): ByteArray? = transact {
        select { Users.id eq userId }.first()[pic]
    }

    private fun readPrimaryKey(userId: Int): Int = transact {
        select { Users.id eq userId }.first()[Users.id].value
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
    fun delete(userId: Int): Unit = transact {
        deleteWhere { Users.id eq userId }
    }
}