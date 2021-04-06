package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.userId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
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
        val type = PicType.valueOf(call.parameters["pic-type"]!!)
        if (!Users.isExisting(userId)) call.respond(HttpStatusCode.BadRequest)
        else {
            val pic = Users.read(userId).pic
            if (pic == null) call.respond(HttpStatusCode.NoContent)
            else
                when (type) {
                    PicType.ORIGINAL -> call.respond(pic.original)
                    PicType.THUMBNAIL -> call.respond(pic.thumbnail)
                }
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
