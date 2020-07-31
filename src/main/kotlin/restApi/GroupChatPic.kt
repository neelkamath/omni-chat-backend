package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.InvalidFileUpload
import com.neelkamath.omniChat.db.tables.Chats
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondBytes
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
        if (!Chats.exists(chatId)) call.respond(HttpStatusCode.BadRequest)
        else {
            val pic = GroupChats.readPic(chatId)
            if (pic == null) call.respond(HttpStatusCode.NoContent) else call.respondBytes(pic.bytes)
        }
    }
}

private fun patchGroupChatPic(route: Route): Unit = with(route) {
    patch {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val pic = readPic()
        when {
            pic == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidFileUpload(InvalidFileUpload.Reason.INVALID_FILE))
            chatId !in GroupChatUsers.readChatIdList(call.userId!!) ->
                call.respond(HttpStatusCode.BadRequest, InvalidFileUpload(InvalidFileUpload.Reason.USER_NOT_IN_CHAT))
            !GroupChatUsers.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.updatePic(chatId, pic)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}