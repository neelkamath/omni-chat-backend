package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ContactsData
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
        val userIdList = call.receive<UserIdList>().userIdList.filter { it in saved }.toSet()
        ContactsData.delete(call.userId, UserIdList(userIdList))
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.get() {
    get {
        val users = ContactsData.read(call.userId).userIdList.map { Auth.findUserById(it) }
        val infoList = users.map { UserPublicInfo(it.id, it.username, it.email, it.firstName, it.lastName) }
        call.respond(UserPublicInfoList(infoList))
    }
}

private fun Route.post() {
    post {
        val saved = ContactsData.read(call.userId).userIdList
        val userIdList = call.receive<UserIdList>().userIdList.filter { it !in saved && it != call.userId }.toSet()
        if (!userIdList.all { it in Auth.getUserIdList() }) call.respond(HttpStatusCode.BadRequest)
        else {
            ContactsData.create(call.userId, UserIdList(userIdList))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}