package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.Users.MAX_PIC_BYTES
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

class User(
    val id: Int,
    val uuid: UUID,
    val bio: Bio? = null,
    val pic: ByteArray? = null,
    val isOnline: Boolean = false,
    /** Ignore if [isOnline]. `null` if the user's never been online. */
    val lastOnline: LocalDateTime? = null
)

/** Replacement for Keycloak's cumbersome custom user attributes. Pics cannot exceed [MAX_PIC_BYTES]. */
object Users : IntIdTable() {
    /** The ID given to the user by Keycloak. */
    private val uuid: Column<UUID> = uuid("uuid")

    /** Bios cannot exceed this many characters. */
    const val MAX_BIO_LENGTH = 2500

    private val bio: Column<String?> = varchar("bio", MAX_BIO_LENGTH).nullable()

    /** Pics cannot exceed 100 KiB. */
    const val MAX_PIC_BYTES = 100 * 1024

    private val pic: Column<ByteArray?> = binary("pic", MAX_PIC_BYTES).nullable()
    private val isOnline: Column<Boolean> = bool("is_online").clientDefault { false }
    private val lastOnline: Column<LocalDateTime?> = datetime("last_online").nullable()

    /**
     * @see [createUser]
     * @see [setOnlineStatus]
     * @see [updatePic]
     */
    fun create(uuid: String, bio: Bio?): Unit = transaction {
        insert {
            it[this.uuid] = UUID.fromString(uuid)
            it[this.bio] = bio?.value
        }
    }

    fun exists(id: Int): Boolean = transaction {
        select { Users.id eq id }.empty().not()
    }

    fun read(id: Int): User = transaction {
        select { Users.id eq id }.first()
    }.let(::buildUser)

    fun read(uuid: String): User = transaction {
        select { Users.uuid eq UUID.fromString(uuid) }.first()
    }.let(::buildUser)

    private fun buildUser(row: ResultRow): User =
        User(row[id].value, row[uuid], row[bio]?.let(::Bio), row[pic], row[isOnline], row[lastOnline])

    /** [Broker.notify]s [Broker.subscribe]rs if [isOnline] differs from the user's current status. */
    fun setOnlineStatus(id: Int, isOnline: Boolean): Unit = transaction {
        if (select { Users.id eq id }.first()[Users.isOnline] == isOnline) return@transaction
        update({ Users.id eq id }) {
            it[Users.isOnline] = isOnline
            it[lastOnline] = LocalDateTime.now()
        }
        onlineStatusesBroker.notify(UpdatedOnlineStatus(id, isOnline)) {
            id != it.userId && (id in Contacts.readIdList(it.userId) || shareChat(id, it.userId))
        }
    }

    /** Calls [negotiateUserUpdate]. */
    fun updateBio(id: Int, bio: Bio?) {
        transaction {
            update({ Users.id eq id }) { it[Users.bio] = bio?.value }
        }
        negotiateUserUpdate(id)
    }

    /** Deletes the pic if [pic] is `null`. Calls [negotiateUserUpdate]. */
    fun updatePic(userId: Int, pic: ByteArray?) {
        transaction {
            update({ Users.id eq userId }) { it[Users.pic] = pic }
        }
        negotiateUserUpdate(userId)
    }

    private fun readPrimaryKey(userId: Int): Int = transaction {
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
     * Deletes the specified user from this table.
     *
     * @see [deleteUser]
     */
    fun delete(id: Int): Unit = transaction {
        deleteWhere { Users.id eq id }
    }
}