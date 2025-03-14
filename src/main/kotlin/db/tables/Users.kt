package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.OnlineStatus
import com.neelkamath.omniChatBackend.graphql.routing.*
import com.neelkamath.omniChatBackend.toLinkedHashSet
import org.jasypt.util.password.StrongPasswordEncryptor
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction

/** Neither usernames nor email addresses may be used more than once. Images cannot exceed [ProcessedImage.ORIGINAL_MAX_BYTES]. */
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
    private val bio: Column<String> = varchar("bio", Bio.MAX_LENGTH)
    private val imageId: Column<Int?> = integer("image_id").references(Images.id).nullable()

    /**
     * @see isUsernameTaken
     * @see isEmailAddressTaken
     * @see setOnlineStatus
     * @see updateImage
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
        val row = select(Users.emailAddress eq emailAddress).first()
        if (row[Users.emailAddressVerificationCode] == emailAddressVerificationCode) {
            update({ Users.emailAddress eq emailAddress }) { it[hasVerifiedEmailAddress] = true }
            true
        } else false
    }

    /** Returns `false` if the [Login.username] doesn't exist, or the [Login.password] is incorrect. */
    fun isValidLogin(login: Login): Boolean = transaction {
        val row = select(username eq login.username.value)
        if (row.empty()) false
        else StrongPasswordEncryptor().checkPassword(login.password.value, row.first()[passwordDigest])
    }

    fun isUsernameTaken(username: Username): Boolean =
        transaction { select(Users.username eq username.value).empty().not() }

    fun isEmailAddressTaken(emailAddress: String): Boolean =
        transaction { select(Users.emailAddress eq emailAddress).empty().not() }

    fun isExisting(userId: Int): Boolean = transaction { select(Users.id eq userId).empty().not() }

    fun isOnline(userId: Int): Boolean = transaction { select(Users.id eq userId).first()[isOnline] }

    /**
     * Subscribers get notified via [onlineStatusesNotifier] and [chatOnlineStatusesNotifier] unless the [userId]'s
     * status was already [isOnline].
     */
    fun setOnlineStatus(userId: Int, isOnline: Boolean): Unit = transaction {
        val status = transaction { select(Users.id eq userId).first()[Users.isOnline] }
        if (status == isOnline) return@transaction
        update({ Users.id eq userId }) {
            it[Users.isOnline] = isOnline
        }
        val update = OnlineStatus(userId)
        val subscribers = Contacts.readOwnerUserIdList(userId).plus(readChatSharers(userId)).map(::UserId)
        onlineStatusesNotifier.publish(update, subscribers)
        val chatIdList = GroupChatUsers.readChatIdList(userId).filter(GroupChats::isExistingPublicChat).map(::ChatId)
        chatOnlineStatusesNotifier.publish(update, chatIdList)
    }

    /**
     * Updates the password of the account associated with the [emailAddress] to the [newPassword] if the
     * [passwordResetCode] is correct, and returns `true`. Otherwise, `false` is returned.
     */
    fun resetPassword(emailAddress: String, passwordResetCode: Int, newPassword: Password): Boolean = transaction {
        val row = select(Users.emailAddress eq emailAddress).first()
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
                update.password?.let { statement[passwordDigest] = StrongPasswordEncryptor().encryptPassword(it.value) }
                update.firstName?.let { statement[firstName] = it.value }
                update.lastName?.let { statement[lastName] = it.value }
                update.bio?.let { statement[bio] = it.value }
            }
        }
        negotiateUserUpdate(userId, isProfileImage = false)
    }

    /** If the [emailAddress] differs from the [userId]'s current one, it'll be marked as unverified. */
    private fun updateEmailAddress(userId: Int, emailAddress: String): Unit = transaction {
        val address = transaction { select(Users.id eq userId).first()[Users.emailAddress] }
        if (emailAddress != address)
            update({ Users.id eq userId }) {
                it[Users.emailAddress] = emailAddress
                it[hasVerifiedEmailAddress] = false
            }
    }

    /** Deletes the [image] if it's `null`. Calls [negotiateUserUpdate]. */
    fun updateImage(userId: Int, image: ProcessedImage?) {
        transaction {
            val op = Users.id eq userId
            update({ op }) { it[imageId] = null }
            val imageId = select(op).first()[imageId]
            update({ op }) { it[this.imageId] = Images.update(imageId, image) }
        }
        negotiateUserUpdate(userId, isProfileImage = true)
    }

    /**
     * Case-insensitively [query]s every user's username, first name, last name, and email address. Returns the IDs
     * of the matched users sorted in ascending order.
     */
    fun searchAll(query: String): LinkedHashSet<Int> = transaction {
        select(buildQuery(query)).orderBy(Users.id).map { it[Users.id].value }.toLinkedHashSet()
    }

    /**
     * Case-insensitively [query]s the username, first name, last name, and email address of each user in the
     * [userIdList]. Returns the IDs of the matched users sorted in ascending order.
     */
    fun search(query: String, userIdList: Collection<Int>): LinkedHashSet<Int> = transaction {
        select((Users.id inList userIdList) and buildQuery(query))
            .orderBy(Users.id)
            .map { it[Users.id].value }
            .toLinkedHashSet()
    }

    private fun buildQuery(query: String): Op<Boolean> =
        (username iLike query) or (firstName iLike query) or (lastName iLike query) or (emailAddress iLike query)

    /**
     * Deletes the specified user if they exist.
     *
     * @see deleteUser
     */
    fun delete(userId: Int): Unit = transaction {
        deleteWhere { Users.id eq userId }
    }

    fun readUsername(userId: Int): Username =
        transaction { select(Users.id eq userId).first()[username].let(::Username) }

    fun readEmailAddress(userId: Int): String = transaction { select(Users.id eq userId).first() }[emailAddress]

    fun readFirstName(userId: Int): Name = transaction { select(Users.id eq userId).first()[firstName].let(::Name) }

    fun readLastName(userId: Int): Name = transaction { select(Users.id eq userId).first()[lastName].let(::Name) }

    fun readBio(userId: Int): Bio = transaction { select(Users.id eq userId).first()[bio].let(::Bio) }

    fun readEmailAddressVerificationCode(userId: Int): Int =
        transaction { select(Users.id eq userId).first()[emailAddressVerificationCode] }

    fun readEmailAddressVerificationCode(emailAddress: String): Int =
        transaction { select(Users.emailAddress eq emailAddress).first()[emailAddressVerificationCode] }

    fun readPasswordResetCode(emailAddress: String): Int =
        transaction { select(Users.emailAddress eq emailAddress).first()[passwordResetCode] }

    fun hasVerifiedEmailAddress(userId: Int): Boolean =
        transaction { select(Users.id eq userId).first()[hasVerifiedEmailAddress] }

    fun hasVerifiedEmailAddress(emailAddress: String): Boolean =
        transaction { select(Users.emailAddress eq emailAddress).first()[hasVerifiedEmailAddress] }

    fun readId(username: Username): Int =
        transaction { select(Users.username eq username.value).first()[Users.id].value }

    fun readImage(userId: Int, type: ImageType): ImageFile? {
        val imageId = transaction { select(Users.id eq userId).first()[imageId] } ?: return null
        return Images.read(imageId, type)
    }
}
