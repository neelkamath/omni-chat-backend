package com.neelkamath.omniChat

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
    AuthzClient.create(config).obtainAccessToken(login.username, login.password)
    true
} catch (exception: HttpResponseException) {
    if (exception.reasonPhrase == "Unauthorized") false else throw exception
}

fun userIdExists(id: String): Boolean = id in realm.users().list().map { it.id }

fun emailAddressExists(email: String): Boolean = email in realm.users().list().map { it.email }

/** Creates a new account, and sends the user a verification email. */
fun createUser(account: NewAccount) {
    realm.users().create(createUserRepresentation(account))
    val userId = findUserByUsername(account.username).id
    sendEmailAddressVerification(userId)
}

/** Sends an email to the user to verify their email address. */
fun sendEmailAddressVerification(userId: String): Unit = realm.users().get(userId).sendVerifyEmail()

/** Sends an email for the user to reset their password. */
fun resetPassword(email: String) {
    val userId = findUserByEmail(email).id
    realm.users().get(userId).executeActionsEmail(listOf("UPDATE_PASSWORD"))
}

private fun createUserRepresentation(account: NewAccount): UserRepresentation = UserRepresentation().apply {
    username = account.username
    credentials = createCredentials(account.password)
    email = account.emailAddress
    firstName = account.firstName
    lastName = account.lastName
    isEnabled = true
}

fun findUserByUsername(username: String): UserRepresentation =
    realm.users().search(username).first { it.username == username }

fun findUserById(userId: String): UserRepresentation = realm.users().list().first { it.id == userId }

private fun findUserByEmail(email: String): UserRepresentation = realm.users().list().first { it.email == email }

/**
 * Case-insensitively [query] the users.
 *
 * The [query] is matched against the [UserRepresentation.username], [UserRepresentation.firstName],
 * [UserRepresentation.lastName], and [UserRepresentation.email].
 */
fun searchUsers(query: String): List<UserRepresentation> = with(realm.users()) {
    searchBy(username = query) +
            searchBy(firstName = query) +
            searchBy(lastName = query) +
            searchBy(emailAddress = query)
}.distinctBy { it.id }

/** Convenience function for [UsersResource.search] which provides only the useful (named) parameters. */
private fun UsersResource.searchBy(
    username: String? = null,
    firstName: String? = null,
    lastName: String? = null,
    emailAddress: String? = null
): List<UserRepresentation> = search(username, firstName, lastName, emailAddress, null, null)

fun updateUser(id: String, update: AccountUpdate) {
    val user = findUserById(id)
    if (update.emailAddress != null && user.email != update.emailAddress) user.isEmailVerified = false
    updateUserRepresentation(user, update)
    realm.users().get(id).update(user)
}

fun isUsernameTaken(username: String): Boolean {
    val results = realm.users().search(username)
    return results.isNotEmpty() && results.any { it.username == username }
}

fun deleteUserFromAuth(id: String) {
    realm.users().delete(id)
}

/** [update]s the [user] in-place. */
private fun updateUserRepresentation(user: UserRepresentation, update: AccountUpdate) {
    user.apply {
        update.username?.let { username = it }
        update.password?.let { credentials = createCredentials(update.password) }
        update.emailAddress?.let { email = it }
        update.firstName?.let { firstName = it }
        update.lastName?.let { lastName = it }
    }
}

private fun createCredentials(password: String): List<CredentialRepresentation> = listOf(
    CredentialRepresentation().apply {
        type = CredentialRepresentation.PASSWORD
        value = password
    }
)