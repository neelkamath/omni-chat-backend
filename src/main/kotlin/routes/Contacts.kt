package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.Contacts
import com.neelkamath.omniChat.db.ContactsData
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.routeContacts() {
    route("contacts") {
        delete()
        get()
        post()
    }
}

private fun Route.delete() {
    delete {
        val saved = ContactsData.read(call.userId).userIdList
        val userIdList = call.receive<Contacts>().userIdList.filter { it in saved }.toSet()
        ContactsData.delete(call.userId, Contacts(userIdList))
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.get() {
    get { call.respond(ContactsData.read(call.userId)) }
}

private fun Route.post() {
    post {
        val saved = ContactsData.read(call.userId).userIdList
        val userIdList = call.receive<Contacts>().userIdList.filter { it !in saved && it != call.userId }.toSet()
        if (!userIdList.all { it in Auth.getUserIdList() }) call.respond(HttpStatusCode.BadRequest)
        else {
            ContactsData.create(call.userId, Contacts(userIdList))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}