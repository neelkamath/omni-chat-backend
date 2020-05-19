package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.findUserByUsername
import com.neelkamath.omniChat.resetPassword
import com.neelkamath.omniChat.sendEmailAddressVerification
import com.neelkamath.omniChat.setUpAuth
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
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

/** Use this instead of [setUpAuth] for the testing environment so that the sending of emails is mocked. */
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

/** Deletes the app's realm. */
fun tearDownAuth(): Unit = realm.remove()

/** Sets the [username]'s email status to verified without sending them an email. */
fun verifyEmailAddress(username: String) {
    val user = findUserByUsername(username)
    realm.users().get(user.id).update(user.apply { isEmailVerified = true })
}