package com.neelkamath.omniChat.routing

import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.route

fun routeHealthCheck(routing: Routing): Unit = with(routing) {
    get("health-check") { call.respond(HttpStatusCode.NoContent) }
}

fun routeProfilePic(routing: Routing): Unit = with(routing) {
    authenticate {
        route("profile-pic") {
            get { call.respond(Users.readProfilePic(call.userId!!) ?: HttpStatusCode.NoContent) }
            patch {
                val part = call.receiveMultipart().readPart()!! as PartData.FileItem
                val profilePic = part.streamProvider().use { it.readBytes() }
                part.dispose()
                if (profilePic.size > Users.MAX_PROFILE_PIC_BYTES) call.respond(HttpStatusCode.BadRequest)
                else {
                    Users.updateProfilePic(call.userId!!, profilePic)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}