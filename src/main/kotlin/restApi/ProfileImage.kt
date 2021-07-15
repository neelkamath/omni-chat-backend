package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.userId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun routeProfileImage(routing: Routing): Unit = with(routing) {
    route("profile-image") {
        getProfileImage(this)
        authenticate { patchProfileImage(this) }
    }
}

private fun getProfileImage(route: Route): Unit = with(route) {
    get {
        val userId = call.parameters["user-id"]!!.toInt()
        val type = ImageType.valueOf(call.parameters["image-type"]!!)
        if (!Users.isExisting(userId)) call.respond(HttpStatusCode.BadRequest)
        else {
            val file = Users.readImage(userId, type)
            if (file == null) call.respond(HttpStatusCode.NoContent)
            else respondDownloadableFile(file.filename, file.bytes, FileDisposition.INLINE)
        }
    }
}

private fun patchProfileImage(route: Route): Unit = with(route) {
    patch {
        val image = readMultipartImage()
        if (image == null) call.respond(HttpStatusCode.BadRequest)
        else {
            Users.updateImage(call.userId!!, image)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
