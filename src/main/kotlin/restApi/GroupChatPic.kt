package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.PicType
import com.neelkamath.omniChatBackend.db.tables.GroupChatUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.userId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun routeGroupChatPic(routing: Routing): Unit = with(routing) {
    route("group-chat-pic") {
        getGroupChatPic(this)
        authenticate { patchGroupChatPic(this) }
    }
}

private fun getGroupChatPic(route: Route): Unit = with(route) {
    get {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val type = PicType.valueOf(call.parameters["pic-type"]!!)
        if (GroupChats.isExisting(chatId)) {
            val pic = GroupChats.readPic(chatId, type)
            if (pic == null) call.respond(HttpStatusCode.NoContent) else call.respondBytes(pic)
        } else call.respond(HttpStatusCode.BadRequest)
    }
}

private fun patchGroupChatPic(route: Route): Unit = with(route) {
    patch {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val pic = readMultipartPic()
        when {
            pic == null -> call.respond(HttpStatusCode.BadRequest)
            !GroupChatUsers.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.updatePic(chatId, pic)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
