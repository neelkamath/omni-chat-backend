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
    lateinit var realm: RealmResource
    const val realmName = "omni-chat"
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
    } catch (_: HttpResponseException) {
        null
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

    fun createUser(user: User) {
        realm.users().create(UserRepresentation().apply { merge(user) })
    }

    fun findUserByUsername(username: String): UserRepresentation = realm.users().search(username)[0]

    fun findUserById(userId: String): UserRepresentation = realm.users().list().first { it.id == userId }

    fun getUserIdList(): List<String> = realm.users().list().map { it.id }

    fun updateUser(id: String, user: User) {
        val representation = findUserById(id)
        if (user.email != null && representation.email != user.email) representation.isEmailVerified = false
        realm.users().get(id).update(representation.apply { merge(user) })
    }

    fun isUsernameTaken(username: String): Boolean = realm.users().search(username).isNotEmpty()

    fun deleteUser(id: String) {
        realm.users().delete(id)
    }

    private fun UserRepresentation.merge(user: User) {
        user.login?.let { login ->
            login.username?.let { username = it }
            login.password?.let {
                credentials = listOf(
                    CredentialRepresentation().apply {
                        type = CredentialRepresentation.PASSWORD
                        value = it
                    }
                )
            }
        }
        user.email?.let { email = it }
        user.firstName?.let { firstName = it }
        user.lastName?.let { lastName = it }
        isEnabled = true
    }
}