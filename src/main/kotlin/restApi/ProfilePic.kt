package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*

fun routeProfilePic(routing: Routing): Unit = with(routing) {
    route("profile-pic") {
        getProfilePic(this)
        authenticate { patchProfilePic(this) }
    }
}

private fun getProfilePic(route: Route): Unit = with(route) {
    get {
        val userId = call.parameters["user-id"]!!.toInt()
        if (!Users.exists(userId)) call.respond(HttpStatusCode.BadRequest)
        else {
            val pic = Users.read(userId).pic
            if (pic == null) call.respond(HttpStatusCode.NoContent) else call.respondBytes(pic.bytes)
        }
    }
}

private fun patchProfilePic(route: Route): Unit = with(route) {
    patch {
        val pic = readMultipartPic()
        if (pic == null) call.respond(HttpStatusCode.BadRequest)
        else {
            Users.updatePic(call.userId!!, pic)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}