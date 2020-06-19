package com.neelkamath.omniChat

import io.mockk.*
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource

private val realm: RealmResource = KeycloakBuilder
    .builder()
    .serverUrl("${System.getenv("KEYCLOAK_URL")}/auth")
    .realm("master")
    .username(System.getenv("KEYCLOAK_USER"))
    .password(System.getenv("KEYCLOAK_PASSWORD"))
    .clientId("admin-cli")
    .build()
    .realm("omni-chat")

/** Sets up account management, and mocks the sending of emails. */
fun setUpAuthForTests() {
    setUpAuth()
    mockEmails()
}

/** Mocks the sending of emails. */
private fun mockEmails() {
    mockkStatic("com.neelkamath.omniChat.AuthKt")
    every { sendEmailAddressVerification(any()) } just runs
    every { resetPassword(any()) } just runs
}

/** Deletes every user. */
fun wipeAuth(): Unit = with(realm.users()) {
    list().forEach { delete(it.id) }
}

/** Deletes the app's realm, and stops mocking emails. */
fun tearDownAuth() {
    realm.remove()
    unmockkStatic("com.neelkamath.omniChat.AuthKt")
}

/** Sets the [username]'s email status to verified without sending them an email. */
fun verifyEmailAddress(username: String) {
    val user = realm.users().list().first { it.username == username }.apply { isEmailVerified = true }
    realm.users().get(user.id).update(user)
}