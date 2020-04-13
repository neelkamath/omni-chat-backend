package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.PrivateChats
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post

fun Route.createGroupChat() {
    post("group-chat") {
        val chat = call.receive<GroupChat>()
        val userIdList = chat.userIdList.filter { it != call.userId }
        val reason = when {
            userIdList.isEmpty() -> InvalidGroupChatReason.EMPTY_USER_ID_LIST
            !userIdList.all { Auth.userIdExists(it) } -> InvalidGroupChatReason.INVALID_USER_ID
            chat.title.isEmpty() || chat.title.length > GroupChats.maxTitleLength ->
                InvalidGroupChatReason.INVALID_TITLE_LENGTH
            chat.description != null && chat.description.length > GroupChats.maxDescriptionLength ->
                InvalidGroupChatReason.INVALID_DESCRIPTION_LENGTH
            else -> null
        }
        if (reason == null) {
            val id = GroupChats.create(call.userId, chat)
            call.respond(ChatId(id))
        } else call.respond(HttpStatusCode.BadRequest, InvalidGroupChat(reason))
    }
}

fun Route.createPrivateChat() {
    post("private-chat") {
        val invitedUserId = call.parameters["user_id"]!!
        if (Auth.userIdExists(invitedUserId) && invitedUserId != call.userId) {
            val id = PrivateChats.create(call.userId, invitedUserId)
            call.respond(ChatId(id))
        } else call.respond(HttpStatusCode.BadRequest)
    }
}

fun Route.readChats() {
    get("chats") {
        val groupChats = GroupChats.read(call.userId).map { Chat(ChatType.GROUP, it.id) }
        val privateChats = PrivateChats.read(call.userId).map { Chat(ChatType.PRIVATE, it.id) }
        call.respond(Chats(groupChats + privateChats))
    }
}