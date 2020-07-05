package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.db.tables.Users
import io.ktor.http.HttpStatusCode
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.Configuration
import org.keycloak.authorization.client.util.HttpResponseException
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation

const val USER_ID_LENGTH = 36
private const val REALM_NAME = "omni-chat"
private const val CLIENT_ID = "server"
private val config: Configuration = Configuration().apply {
    realm = REALM_NAME
    authServerUrl = "${System.getenv("KEYCLOAK_URL")}/auth"
    sslRequired = "external"
    resource = CLIENT_ID
    credentials = mapOf("secret" to System.getenv("KEYCLOAK_CLIENT_SECRET"))
    confidentialPort = 0
}
private val keycloak: Keycloak = KeycloakBuilder
    .builder()
    .serverUrl("${System.getenv("KEYCLOAK_URL")}/auth")
    .realm("master")
    .username(System.getenv("KEYCLOAK_USER"))
    .password(System.getenv("KEYCLOAK_PASSWORD"))
    .clientId("admin-cli")
    .build()
private lateinit var realm: RealmResource
private val omniChatRealm: Lazy<RealmRepresentation> = lazy {
    RealmRepresentation().apply {
        realm = REALM_NAME
        displayName = "Omni Chat"
        isEnabled = true
        isVerifyEmail = true
        isEditUsernameAllowed = true
        smtpServer = mapOf(
            "host" to System.getenv("KEYCLOAK_SMTP_HOST"),
            "port" to System.getenv("KEYCLOAK_SMTP_TLS_PORT"),
            "from" to System.getenv("KEYCLOAK_SMTP_USER"),
            "starttls" to "true",
            "auth" to "true",
            "user" to System.getenv("KEYCLOAK_SMTP_USER"),
            "password" to System.getenv("KEYCLOAK_SMTP_PASSWORD")
        )
    }
}

/**
 * Sets up account management.
 *
 * This must be run before any account-related activities are performed. This takes a small, but noticeable amount of
 * time.
 */
fun setUpAuth() {
    val shouldBuild = REALM_NAME !in keycloak.realms().findAll().map { it.realm }
    if (shouldBuild) keycloak.realms().create(omniChatRealm.value)
    realm = keycloak.realm(REALM_NAME)
    if (shouldBuild) createClient()
}

/** Creates the client used to access the [realm] programmatically. Don't call this if the client exists.  */
private fun createClient() {
    realm.clients().create(
        ClientRepresentation().apply {
            clientId = CLIENT_ID
            secret = System.getenv("KEYCLOAK_CLIENT_SECRET")
            isDirectAccessGrantsEnabled = true
            rootUrl = System.getenv("KEYCLOAK_URL")
        }
    )
}

/** Returns whether the [login] is valid. */
fun isValidLogin(login: Login): Boolean = try {
    AuthzClient.create(config).obtainAccessToken(login.username.value, login.password.value)
    true
} catch (exception: HttpResponseException) {
    /*
    Keycloak returns an HTTP status code of 400 (Bad Request) or 401 (Unauthorized) if the login is invalid. It's
    possible that a Bad Request is returned due to a bug on Omni Chat's server (i.e., the login is valid). However,
    were this to occur, every request would be invalid, and it would be obvious that there's a bug.
     */
    if (exception.statusCode in listOf(HttpStatusCode.Unauthorized.value, HttpStatusCode.BadRequest.value)) false
    else throw exception
}

fun userIdExists(id: String): Boolean = id in realm.users().list().map { it.id }

fun emailAddressExists(email: String): Boolean = email in realm.users().list().map { it.email }

/**
 * Creates a new account in the auth system, and sends the user a verification email.
 *
 * @return the user ID.
 * @see [Users.create]
 */
fun createUser(account: NewAccount): String {
    realm.users().create(createUserRepresentation(account))
    val userId = readUserByUsername(account.username).id
    sendEmailAddressVerification(userId)
    return userId
}

/** Sends an email to the user to verify their email address. */
fun sendEmailAddressVerification(userId: String): Unit = realm.users().get(userId).sendVerifyEmail()

/** Sends an email for the user to reset their password. */
fun resetPassword(email: String) {
    val userId = readUserByEmail(email).id
    realm.users().get(userId).executeActionsEmail(listOf("UPDATE_PASSWORD"))
}

private fun createUserRepresentation(account: NewAccount): UserRepresentation = UserRepresentation().apply {
    username = account.username.value
    credentials = createCredentials(account.password)
    email = account.emailAddress
    firstName = account.firstName
    lastName = account.lastName
    isEnabled = true
}

private fun buildAccount(user: UserRepresentation): Account =
    with(user) { Account(id, Username(username), email, firstName, lastName) }

fun readUserByUsername(username: Username): Account =
    realm.users().search(username.value).first { it.username == username.value }.let(::buildAccount)

fun isEmailVerified(userId: String): Boolean = readUser(userId).isEmailVerified

fun readUserById(userId: String): Account = buildAccount(readUser(userId))

private fun readUser(userId: String): UserRepresentation = realm.users().list().first { it.id == userId }

private fun readUserByEmail(email: String): Account =
    realm.users().list().first { it.email == email }.let(::buildAccount)

/**
 * Case-insensitively [query]s every user's username, first name, last name, and email address.
 *
 * @see [Users.search]
 */
fun searchUsers(query: String): List<Account> = with(realm.users()) {
    searchBy(username = query) +
            searchBy(firstName = query) +
            searchBy(lastName = query) +
            searchBy(emailAddress = query)
}.distinctBy { it.id }.map(::buildAccount)

/** Convenience function for [UsersResource.search] which provides only the relevant parameters with names. */
private fun UsersResource.searchBy(
    username: String? = null,
    firstName: String? = null,
    lastName: String? = null,
    emailAddress: String? = null
): List<UserRepresentation> = search(username, firstName, lastName, emailAddress, null, null)

/**
 * Clients who have [Broker.subscribe]d via [contactsBroker], [groupChatInfoBroker], and [privateChatInfoBroker] will be
 * [Broker.notify]d of the user's [update].
 */
fun updateUser(id: String, update: AccountUpdate) {
    val user = readUser(id)
    if (update.emailAddress != null && user.email != update.emailAddress) user.isEmailVerified = false
    updateUserRepresentation(user, update)
    realm.users().get(id).update(user)
    contactsBroker.notify(UpdatedContact.fromUserId(id)) { id in Contacts.readIdList(it.userId) }
    val account = UpdatedAccount.fromUserId(id)
    privateChatInfoBroker.notify(account) { id in PrivateChats.readOtherUserIdList(it.userId) }
    groupChatInfoBroker.notify(account) { it.userId != id && GroupChatUsers.areInSameChat(it.userId, id) }
}

fun isUsernameTaken(username: Username): Boolean {
    val results = realm.users().search(username.value)
    return results.isNotEmpty() && results.any { it.username == username.value }
}

/**
 * @see [Users.delete]
 * @see [deleteUserFromDb]
 */
fun deleteUserFromAuth(id: String) {
    realm.users().delete(id)
}

/** [update]s the [user] in-place. */
private fun updateUserRepresentation(user: UserRepresentation, update: AccountUpdate) {
    user.apply {
        update.username?.let { username = it.value }
        update.password?.let { credentials = createCredentials(update.password) }
        update.emailAddress?.let { email = it }
        update.firstName?.let { firstName = it }
        update.lastName?.let { lastName = it }
    }
}

private fun createCredentials(password: Password): List<CredentialRepresentation> = listOf(
    CredentialRepresentation().apply {
        type = CredentialRepresentation.PASSWORD
        value = password.value
    }
)