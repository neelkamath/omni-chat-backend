package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.routeContacts() {
    route("contacts") {
        deleteContacts()
        readContacts()
        createContacts()
    }
}

private fun Route.deleteContacts() {
    delete {
        val saved = Contacts.read(call.userId)
        val userIdList = call.receive<UserIdList>().userIdList.filter { it in saved }.toSet()
        Contacts.delete(call.userId, (userIdList))
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.readContacts() {
    get {
        val userIdList = Contacts.read(call.userId)
        call.respond(UserIdList(userIdList))
    }
}

private fun Route.createContacts() {
    post {
        val saved = Contacts.read(call.userId)
        val userIdList = call.receive<UserIdList>().userIdList.filter { it !in saved && it != call.userId }.toSet()
        if (!userIdList.all { it in Auth.getUserIdList() }) call.respond(HttpStatusCode.BadRequest)
        else {
            Contacts.create(call.userId, userIdList)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}