package com.neelkamath.omniChat

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.ClientAuthenticator
import org.keycloak.authorization.client.Configuration
import org.keycloak.authorization.client.util.Http
import org.keycloak.authorization.client.util.HttpResponseException
import org.keycloak.representations.AccessTokenResponse
import org.keycloak.representations.idm.ClientRepresentation
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation

object Auth {
    /** This is only exposed for extensions to this object needing it in the test source set. Never use it directly. */
    lateinit var realm: RealmResource
    const val realmName = "omni-chat"
    const val userIdLength = 36
    private const val clientId = "server"
    private val configuration: Configuration = Configuration().apply {
        realm = realmName
        authServerUrl = "${System.getenv("KEYCLOAK_URL")}/auth"
        sslRequired = "external"
        resource = clientId
        credentials = mapOf("secret" to System.getenv("KEYCLOAK_CLIENT_SECRET"))
        confidentialPort = 0
    }

    /**
     * Sets up account management.
     *
     * This must be run before any account-related activities are performed. This takes a small, but noticeable amount
     * of time.
     */
    fun setUp() {
        val keycloak = getMasterRealm()
        val shouldBuild = realmName !in keycloak.realms().findAll().map { it.realm }
        if (shouldBuild) keycloak.realms().create(buildOmniChatRealm())
        realm = keycloak.realm(realmName)
        if (shouldBuild) createClient()
    }

    private fun getMasterRealm(): Keycloak = KeycloakBuilder.builder()
        .serverUrl("${System.getenv("KEYCLOAK_URL")}/auth")
        .realm("master")
        .username(System.getenv("KEYCLOAK_USER"))
        .password(System.getenv("KEYCLOAK_PASSWORD"))
        .clientId("admin-cli")
        .build()

    private fun buildOmniChatRealm(): RealmRepresentation = RealmRepresentation().apply {
        realm = realmName
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

    private fun createClient() {
        realm.clients().create(
            ClientRepresentation().also {
                it.clientId = clientId
                it.secret = System.getenv("KEYCLOAK_CLIENT_SECRET")
                it.isDirectAccessGrantsEnabled = true
                it.rootUrl = System.getenv("KEYCLOAK_URL")
            }
        )
    }

    /** Returns `null` if the [Login.password] is incorrect. */
    fun getToken(login: Login): AccessTokenResponse? = try {
        AuthzClient.create(configuration).obtainAccessToken(login.username, login.password)
    } catch (exception: HttpResponseException) {
        if (exception.reasonPhrase == "Unauthorized") null else throw exception
    }

    fun refreshToken(refreshToken: String): AccessTokenResponse = Http(configuration, ClientAuthenticator { _, _ -> })
        .post<AccessTokenResponse>(
            "${System.getenv("KEYCLOAK_URL")}/auth/realms/$realmName/protocol/openid-connect/token"
        )
        .authentication()
        .client()
        .form()
        .param("grant_type", "refresh_token")
        .param("refresh_token", refreshToken)
        .param("client_id", clientId)
        .param("client_secret", System.getenv("KEYCLOAK_CLIENT_SECRET"))
        .response()
        .json(AccessTokenResponse::class.java)
        .execute()

    fun usernameExists(username: String): Boolean = realm.users().search(username).size == 1

    fun userIdExists(id: String): Boolean = realm.users().list().map { it.id }.contains(id)

    fun emailExists(email: String): Boolean = realm.users().list().map { it.email }.contains(email)

    /** Creates a new account, and sends the user a verification email. */
    fun createUser(account: NewAccount) {
        realm.users().create(createUserRepresentation(account))
        sendEmailVerification(findUserByUsername(account.username).id)
    }

    /** Sends an email to the user to verify their email address. This does nothing if [isTestingEnvironment]. */
    fun sendEmailVerification(userId: String) {
        if (!isTestingEnvironment) realm.users().get(userId).sendVerifyEmail()
    }

    /** Sends an email for the user to reset their password. This does nothing if [isTestingEnvironment]. */
    fun resetPassword(email: String) {
        if (!isTestingEnvironment)
            realm.users().get(findUserByEmail(email).id).executeActionsEmail(listOf("UPDATE_PASSWORD"))
    }

    private fun createUserRepresentation(account: NewAccount): UserRepresentation = UserRepresentation().apply {
        username = account.username
        credentials = createCredentials(account.password)
        email = account.email
        firstName = account.firstName
        lastName = account.lastName
        isEnabled = true
    }

    fun findUserByUsername(username: String): UserRepresentation = realm.users().search(username)[0]

    fun findUserById(userId: String): UserRepresentation = realm.users().list().first { it.id == userId }

    private fun findUserByEmail(email: String): UserRepresentation =
        realm.users().list().filter { it.email == email }[0]

    /**
     * Case-insensitively searches users with the [query].
     *
     * The [query] is matched against the [UserRepresentation.username], [UserRepresentation.firstName],
     * [UserRepresentation.lastName], and [UserRepresentation.email].
     */
    fun searchUsers(query: String): List<UserRepresentation> =
        searchByUsername(query) + searchByFirstName(query) + searchByLastName(query) + searchByEmail(query)

    /** Case-insensitively searches users by their [username]. */
    private fun searchByUsername(username: String): List<UserRepresentation> = realm.users().search(username)

    /** Case-insensitively searches users by their [firstName]. */
    private fun searchByFirstName(firstName: String): List<UserRepresentation> =
        realm.users().search(null, firstName, null, null, null, null)

    /** Case-insensitively searches users by their [lastName]. */
    private fun searchByLastName(lastName: String): List<UserRepresentation> =
        realm.users().search(null, null, lastName, null, null, null)

    /** Case-insensitively searches users by their [email]. */
    private fun searchByEmail(email: String): List<UserRepresentation> =
        realm.users().search(null, null, null, email, null, null)

    fun getUserIdList(): List<String> = realm.users().list().map { it.id }

    fun updateUser(id: String, update: AccountUpdate) {
        val user = findUserById(id)
        if (update.email != null && user.email != update.email) user.isEmailVerified = false
        updateUserRepresentation(user, update)
        realm.users().get(id).update(user)
    }

    fun isUsernameTaken(username: String): Boolean = realm.users().search(username).isNotEmpty()

    fun deleteUser(id: String) {
        realm.users().delete(id)
    }

    /** [update]s the [user] in-place. */
    private fun updateUserRepresentation(user: UserRepresentation, update: AccountUpdate) {
        user.apply {
            update.username?.let { username = it }
            update.password?.let { credentials = createCredentials(update.password) }
            update.email?.let { email = it }
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
}