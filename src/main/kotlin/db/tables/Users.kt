package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import org.jasypt.util.password.StrongPasswordEncryptor
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

data class User(
        val id: Int,
        val username: Username,
        val passwordResetCode: Int,
        val emailAddress: String,
        val hasVerifiedEmailAddress: Boolean,
        val emailAddressVerificationCode: Int,
        val firstName: Name,
        val lastName: Name,
        val isOnline: Boolean,
        val lastOnline: LocalDateTime?,
        val bio: Bio,
        val pic: Pic?
) {
    fun toAccount(): Account = Account(id, username, emailAddress, firstName, lastName, bio)
}

/** Neither usernames not email addresses may be used more than once. Pics cannot exceed [Pic.MAX_BYTES]. */
object Users : IntIdTable() {
    /** Names and usernames cannot exceed 30 characters. */
    const val MAX_NAME_LENGTH = 30
    private val username: Column<String> = varchar("username", MAX_NAME_LENGTH).uniqueIndex()

    // Password digests are 64 characters regardless of whether the password is longer or shorter than 64 characters.
    private val passwordDigest: Column<String> = varchar("password_digest", 64)
    private val passwordResetCode: Column<Int> =
            integer("password_reset_code").clientDefault { (100_000..999_999).random() }
    private val emailAddress: Column<String> = varchar("email_address", 254).uniqueIndex()
    private val hasVerifiedEmailAddress: Column<Boolean> = bool("has_verified_email_address").clientDefault { false }
    private val emailAddressVerificationCode: Column<Int> =
            integer("email_address_verification_code").clientDefault { (100_000..999_999).random() }
    private val firstName: Column<String> = varchar("first_name", MAX_NAME_LENGTH)
    private val lastName: Column<String> = varchar("last_name", MAX_NAME_LENGTH)
    private val isOnline: Column<Boolean> = bool("is_online").clientDefault { false }
    private val lastOnline: Column<LocalDateTime?> = datetime("last_online").nullable()
    private val bio: Column<String> = varchar("bio", Bio.MAX_LENGTH)
    private val picId: Column<Int?> = integer("pic_id").references(Pics.id).nullable()

    /**
     * Check if [isUsernameTaken] because the [AccountInput.username] mustn't exist.
     *
     * @see [setOnlineStatus]
     * @see [updatePic]
     */
    fun create(account: AccountInput): Unit = transaction {
        insert {
            it[username] = account.username.value
            it[passwordDigest] = StrongPasswordEncryptor().encryptPassword(account.password.value)
            it[emailAddress] = account.emailAddress
            it[firstName] = account.firstName.value
            it[lastName] = account.lastName.value
            it[bio] = account.bio.value
        }
    }

    /**
     * If the [emailAddressVerificationCode] was correct, the account's [emailAddress] is set to verified. Returns
     * whether the [emailAddress] was verified using the [emailAddressVerificationCode].
     */
    fun verifyEmailAddress(emailAddress: String, emailAddressVerificationCode: Int): Boolean = transaction {
        val row = select { Users.emailAddress eq emailAddress }.first()
        if (row[Users.emailAddressVerificationCode] == emailAddressVerificationCode) {
            update({ Users.emailAddress eq emailAddress }) { it[hasVerifiedEmailAddress] = true }
            true
        } else false
    }

    /** Returns `false` if the [Login.username] doesn't exist, or the [Login.password] is incorrect. */
    fun isValidLogin(login: Login): Boolean = transaction {
        val row = select { username eq login.username.value }
        if (row.empty()) return@transaction false
        StrongPasswordEncryptor().checkPassword(login.password.value, row.first()[passwordDigest])
    }

    fun isUsernameTaken(username: Username): Boolean = transaction {
        select { Users.username eq username.value }.empty().not()
    }

    fun isEmailAddressTaken(emailAddress: String): Boolean = transaction {
        select { Users.emailAddress eq emailAddress }.empty().not()
    }

    fun exists(userId: Int): Boolean = transaction {
        select { Users.id eq userId }.empty().not()
    }

    fun read(userId: Int): User = transaction {
        select { Users.id eq userId }.first()
    }.toUser()

    fun read(username: Username): User = transaction {
        select { Users.username eq username.value }.first()
    }.toUser()

    fun read(emailAddress: String): User = transaction {
        select { Users.emailAddress eq emailAddress }.first()
    }.toUser()

    private fun ResultRow.toUser(): User = User(
            this[id].value,
            Username(this[username]),
            this[passwordResetCode],
            this[emailAddress],
            this[hasVerifiedEmailAddress],
            this[emailAddressVerificationCode],
            Name(this[firstName]),
            Name(this[lastName]),
            this[isOnline],
            this[lastOnline],
            Bio(this[bio]),
            this[picId]?.let(Pics::read)
    )

    private fun ResultRow.toAccount(): Account = Account(
            this[id].value,
            Username(this[username]),
            this[emailAddress],
            Name(this[firstName]),
            Name(this[lastName]),
            Bio(this[bio])
    )

    /**
     * Notifies subscribers of the [UpdatedOnlineStatus] only if [isOnline] differs from the [userId]'s current status.
     */
    fun setOnlineStatus(userId: Int, isOnline: Boolean): Unit = transaction {
        if (select { Users.id eq userId }.first()[Users.isOnline] == isOnline) return@transaction
        update({ Users.id eq userId }) {
            it[Users.isOnline] = isOnline
            it[lastOnline] = LocalDateTime.now()
        }
        val subscribers = Contacts.readOwners(userId) + readChatSharers(userId)
        onlineStatusesNotifier.publish(UpdatedOnlineStatus(userId, isOnline), subscribers)
    }

    /**
     * Updates the password of the account associated with the [emailAddress] to the [newPassword] if the
     * [passwordResetCode] is correct, and returns `true`. Otherwise, `false` is returned.
     */
    fun resetPassword(emailAddress: String, passwordResetCode: Int, newPassword: Password): Boolean = transaction {
        val row = select { Users.emailAddress eq emailAddress }.first()
        if (row[Users.passwordResetCode] == passwordResetCode) {
            update({ Users.emailAddress eq emailAddress }) {
                it[passwordDigest] = StrongPasswordEncryptor().encryptPassword(newPassword.value)
            }
            true
        } else false
    }

    /**
     * Calls [negotiateUserUpdate]. If the [AccountUpdate.emailAddress] isn't `null`, and differs from the current one,
     * then the email address is marked as unverified.
     */
    fun update(userId: Int, update: AccountUpdate) {
        update.emailAddress?.let { updateEmailAddress(userId, it) }
        transaction {
            update({ Users.id eq userId }) { statement ->
                update.username?.let { statement[username] = it.value }
                update.password?.let {
                    statement[passwordDigest] =
                            StrongPasswordEncryptor().encryptPassword(update.password.value)
                }
                update.firstName?.let { statement[firstName] = it.value }
                update.lastName?.let { statement[lastName] = it.value }
                update.bio?.let { statement[bio] = it.value }
            }
        }
        negotiateUserUpdate(userId)
    }

    /** If the [emailAddress] differs from the [userId]'s current one, it'll be marked as unverified. */
    private fun updateEmailAddress(userId: Int, emailAddress: String): Unit = transaction {
        val address = select { Users.id eq userId }.first()[Users.emailAddress]
        if (emailAddress != address)
            update({ Users.id eq userId }) {
                it[Users.emailAddress] = emailAddress
                it[hasVerifiedEmailAddress] = false
            }
    }

    /** Deletes the [pic] if it's `null`. Calls [negotiateUserUpdate]. */
    fun updatePic(userId: Int, pic: Pic?) {
        transaction {
            val op = Users.id eq userId
            update({ op }) { it[this.picId] = null }
            val picId = select(op).first()[picId]
            update({ op }) { it[this.picId] = Pics.update(picId, pic) }
        }
        negotiateUserUpdate(userId)
    }

    /** Case-insensitively [query]s every user's username, first name, last name, and email address. */
    fun search(query: String, pagination: ForwardPagination? = null): AccountsConnection {
        val users = transaction {
            selectAll()
                    .orderBy(Users.id)
                    .filter {
                        it[username].contains(query, ignoreCase = true) ||
                                it[firstName].contains(query, ignoreCase = true) ||
                                it[lastName].contains(query, ignoreCase = true) ||
                                it[emailAddress].contains(query, ignoreCase = true)
                    }
                    .map { AccountEdge(it.toAccount(), it[Users.id].value) }
        }
        return AccountsConnection.build(users, pagination)
    }

    /**
     * Deletes the specified user if they exist.
     *
     * @see [deleteUser]
     */
    fun delete(id: Int): Unit = transaction {
        deleteWhere { Users.id eq id }
    }
}
