package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.keycloak.representations.idm.UserRepresentation

fun Route.searchContacts() {
    get("contacts-search") {
        val query = call.parameters["query"]!!
        val contacts = Contacts.read(call.userId).map { Auth.findUserById(it) }.filter { matches(query, it) }
        call.respond(UserIdList(contacts.map { it.id }.toSet()))
    }
}

private fun matches(query: String, user: UserRepresentation): Boolean =
    with(user) { listOfNotNull(username, firstName, lastName, email) }.any { it.contains(query, ignoreCase = true) }