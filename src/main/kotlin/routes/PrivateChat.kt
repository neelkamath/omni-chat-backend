package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.PrivateChatClears
import com.neelkamath.omniChat.db.PrivateChats
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.routePrivateChat() {
    route("private-chat") {
        createPrivateChat()
        deletePrivateChat()
        readPrivateChat()
    }
}

private fun Route.readPrivateChat() {
    get {
        val chatId = call.parameters["chat_id"]!!.toInt()
        val chats = PrivateChats.read(call.userId)
        if (chatId in chats.map { it.id }) {
            val chat = chats.first { it.id == chatId }
            val userId = with(chat) { if (creatorUserId == call.userId) invitedUserId else creatorUserId }
            call.respond(PrivateChat(userId))
        } else call.respond(HttpStatusCode.BadRequest)
    }
}

private fun Route.deletePrivateChat() {
    delete {
        val chatId = call.parameters["chat_id"]!!.toInt()
        if (chatId in PrivateChats.read(call.userId).map { it.id }) {
            PrivateChatClears.create(chatId, PrivateChats.isCreator(chatId, call.userId))
            call.respond(HttpStatusCode.NoContent)
        } else call.respond(HttpStatusCode.BadRequest)
    }
}

private fun Route.createPrivateChat() {
    post {
        val invitedUserId = call.parameters["user_id"]!!
        when {
            PrivateChats.exists(call.userId, invitedUserId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidPrivateChat(InvalidPrivateChatReason.CHAT_EXISTS))
            !Auth.userIdExists(invitedUserId) || invitedUserId == call.userId ->
                call.respond(HttpStatusCode.BadRequest, InvalidPrivateChat(InvalidPrivateChatReason.INVALID_USER_ID))
            else -> {
                val id = PrivateChats.create(call.userId, invitedUserId)
                call.respond(ChatId(id))
            }
        }
    }
}