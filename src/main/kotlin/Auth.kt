package com.neelkamath.omniChat

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.RealmRepresentation
import org.keycloak.representations.idm.UserRepresentation

lateinit var realm: RealmResource

/**
 * Sets up account management.
 *
 * This must be run before any account-related activities are performed. This takes a small, but noticeable amount of
 * time.
 */
fun setUpAuth() {
    realm = with(getMasterRealm()) {
        if ("omni-chat" !in realms().findAll().map { it.realm }) realms().create(createOmniChatRealm())
        realm("omni-chat")
    }
}

private fun getMasterRealm(): Keycloak = KeycloakBuilder.builder()
    .serverUrl(System.getenv("KEYCLOAK_URL"))
    .realm("master")
    .username(System.getenv("KEYCLOAK_USER"))
    .password(System.getenv("KEYCLOAK_PASSWORD"))
    .clientId("admin-cli")
    .build()

private fun createOmniChatRealm(): RealmRepresentation = RealmRepresentation().apply {
    realm = "omni-chat"
    displayName = "Omni Chat"
    isEnabled = true
    isVerifyEmail = true
    smtpServer = mapOf(
        "host" to System.getenv("KEYCLOAK_SMTP_HOST"),
        "port" to System.getenv("KEYCLOAK_SMTP_SSL_PORT"),
        "from" to System.getenv("KEYCLOAK_SMTP_USER"),
        "ssl" to "true",
        "auth" to "true",
        "user" to System.getenv("KEYCLOAK_SMTP_USER"),
        "password" to System.getenv("KEYCLOAK_SMTP_PASSWORD")
    )
}

fun userExists(username: String): Boolean = realm.users().search(username).size == 1

fun createUser(user: User) {
    realm.users().create(
        UserRepresentation().apply {
            username = user.username
            credentials = listOf(
                CredentialRepresentation().apply {
                    type = CredentialRepresentation.PASSWORD
                    value = user.password
                }
            )
            email = user.email
            firstName = user.firstName
            lastName = user.lastName
            isEnabled = true
        }
    )
}