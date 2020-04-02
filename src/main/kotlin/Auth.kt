package com.neelkamath.omnichat

import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.RealmRepresentation

private val keycloak = run {
    val keycloak = KeycloakBuilder.builder()
        .serverUrl(System.getenv("KEYCLOAK_URL"))
        .realm("master")
        .username(System.getenv("KEYCLOAK_USER"))
        .password(System.getenv("KEYCLOAK_PASSWORD"))
        .clientId("admin-cli")
        .build()
    if ("omni-chat" !in keycloak.realms().findAll().map { it.realm })
        keycloak.realms().create(RealmRepresentation().apply { realm = "omni-chat" })
    keycloak.realms().realm("omni-chat")
}