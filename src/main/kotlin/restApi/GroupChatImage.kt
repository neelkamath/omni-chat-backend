package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.ImageType
import com.neelkamath.omniChatBackend.db.tables.GroupChatUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.userId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun routeGroupChatImage(routing: Routing): Unit = with(routing) {
    route("group-chat-image") {
        getGroupChatImage(this)
        authenticate { patchGroupChatImage(this) }
    }
}

private fun getGroupChatImage(route: Route): Unit = with(route) {
    get {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val type = ImageType.valueOf(call.parameters["image-type"]!!)
        if (GroupChats.isExisting(chatId)) {
            val file = GroupChats.readImage(chatId, type)
            if (file == null) call.respond(HttpStatusCode.NoContent)
            else call.respondFile(buildFile(file.filename, file.bytes))
        } else call.respond(HttpStatusCode.BadRequest)
    }
}

private fun patchGroupChatImage(route: Route): Unit = with(route) {
    patch {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val image = readMultipartImage()
        when {
            image == null -> call.respond(HttpStatusCode.BadRequest)
            !GroupChatUsers.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.updateImage(chatId, image)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
