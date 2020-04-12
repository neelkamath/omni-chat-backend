package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Contacts
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
        val saved = Contacts.read(call.userId).userIdList
        val userIdList = call.receive<UserIdList>().userIdList.filter { it in saved }.toSet()
        Contacts.delete(call.userId, UserIdList(userIdList))
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.get() {
    get {
        val users = Contacts.read(call.userId).userIdList.map { Auth.findUserById(it) }
        val infoList = users.map { UserPublicInfo(it.id, it.username, it.email, it.firstName, it.lastName) }
        call.respond(UserPublicInfoList(infoList))
    }
}

private fun Route.post() {
    post {
        val saved = Contacts.read(call.userId).userIdList
        val userIdList = call.receive<UserIdList>().userIdList.filter { it !in saved && it != call.userId }.toSet()
        if (!userIdList.all { it in Auth.getUserIdList() }) call.respond(HttpStatusCode.BadRequest)
        else {
            Contacts.create(call.userId, UserIdList(userIdList))
            call.respond(HttpStatusCode.NoContent)
        }
    }
}