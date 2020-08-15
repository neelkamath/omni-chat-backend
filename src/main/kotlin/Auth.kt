package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.deleteUserFromDb
import com.neelkamath.omniChat.db.negotiateUserUpdate
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.graphql.routing.*
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

fun UserRepresentation.toAccount(): Account {
    val user = Users.read(id)
    return Account(user.id, Username(username), email, firstName, lastName, user.bio)
}

/**
 * Sets up account management.
 *
 * This takes a few seconds to run if the auth system hasn't been set up, and must be run before any account-related
 * activities are performed.
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

/** Whether the [login] is valid. */
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

fun emailAddressExists(email: String): Boolean = email in realm.users().list().map { it.email }

/**
 * Creates a new account in the auth system, saves it to the DB via [Users.create], and sends the user a verification
 * email.
 */
fun createUser(account: AccountInput) {
    realm.users().create(account.toUserRepresentation())
    val userId = realm
        .users()
        .search(account.username.value)
        .first { it.username == account.username.value }
        .id
    Users.create(userId, account.bio)
    sendEmailAddressVerification(account.emailAddress)
}

/** Sends an email to the user to verify their email [address]. */
fun sendEmailAddressVerification(address: String) {
    val userId = readUserByEmailAddress(address).id.let(Users::read).uuid.toString()
    realm.users().get(userId).sendVerifyEmail()
}

/** Sends an email for the user to reset their password. */
fun resetPassword(email: String) {
    val userId = readUserByEmailAddress(email).id
    realm.users().get(Users.read(userId).uuid.toString()).executeActionsEmail(listOf("UPDATE_PASSWORD"))
}

private fun AccountInput.toUserRepresentation(): UserRepresentation = UserRepresentation().also {
    it.username = username.value
    it.credentials = createCredentials(password)
    it.email = emailAddress
    it.firstName = firstName
    it.lastName = lastName
    it.isEnabled = true
}

fun readUserByUsername(username: Username): Account =
    realm.users().search(username.value).first { it.username == username.value }.toAccount()

fun isEmailVerified(userId: Int): Boolean = readUser(userId).isEmailVerified

fun readUserById(userId: Int): Account = readUser(userId).toAccount()

private fun readUser(userId: Int): UserRepresentation = realm.users().list().first { Users.read(it.id).id == userId }

private fun readUserByEmailAddress(email: String): Account =
    realm.users().list().first { it.email == email }.toAccount()

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
}.distinctBy { it.id }.map { it.toAccount() }

/** Convenience function for [UsersResource.search] which provides only the relevant parameters with names. */
private fun UsersResource.searchBy(
    username: String? = null,
    firstName: String? = null,
    lastName: String? = null,
    emailAddress: String? = null
): List<UserRepresentation> = search(username, firstName, lastName, emailAddress, null, null)

/** Calls [negotiateUserUpdate]. */
fun updateUser(id: Int, update: AccountUpdate) {
    val user = readUser(id)
    if (update.emailAddress != null && user.email != update.emailAddress) user.isEmailVerified = false
    user.update(update)
    realm.users().get(Users.read(id).uuid.toString()).update(user)
    Users.updateBio(id, update.bio)
    negotiateUserUpdate(id)
}

fun isUsernameTaken(username: Username): Boolean {
    val results = realm.users().search(username.value)
    return results.isNotEmpty() && results.any { it.username == username.value }
}

/** Deletes the user [id] from the auth system after calling [deleteUserFromDb]. */
fun deleteUser(id: Int) {
    realm.users().delete(Users.read(id).uuid.toString())
    deleteUserFromDb(id)
}

/** Applies this [update]. */
private fun UserRepresentation.update(update: AccountUpdate) {
    update.username?.let { username = it.value }
    update.password?.let { credentials = createCredentials(it) }
    update.emailAddress?.let { email = it }
    update.firstName?.let { firstName = it }
    update.lastName?.let { lastName = it }
}

private fun createCredentials(password: Password): List<CredentialRepresentation> = listOf(
    CredentialRepresentation().apply {
        type = CredentialRepresentation.PASSWORD
        value = password.value
    }
)

/** Whether the [emailAddress]'s domain (e.g., `"example.com"`) is allowed by this Omni Chat instance. */
fun hasAllowedDomain(emailAddress: String): Boolean {
    val domains = System.getenv("ALLOWED_DOMAINS") ?: return true
    return emailAddress.substringAfter("@") in domains.split(",")
}