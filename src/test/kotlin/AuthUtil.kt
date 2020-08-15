package com.neelkamath.omniChat

import com.neelkamath.omniChat.graphql.routing.Username
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

/** Mocks the sending of emails. */
fun mockEmails() {
    mockkStatic("com.neelkamath.omniChat.AuthKt")
    every { sendEmailAddressVerification(any()) } just runs
    every { resetPassword(any()) } just runs
}

/** Deletes every user. */
fun wipeAuth(): Unit = with(realm.users()) {
    list().forEach { delete(it.id) }
}

/** Sets the [username]'s email status to verified without sending them an email. */
fun verifyEmailAddress(username: Username) {
    val user = realm.users().list().first { it.username == username.value }.apply { isEmailVerified = true }
    realm.users().get(user.id).update(user)
}