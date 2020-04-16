package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.routeGroupChat() {
    route("group-chat") {
        createGroupChat()
        updateGroupChat()
        leaveGroupChat()
    }
}

private fun Route.createGroupChat() {
    post {
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

private fun Route.updateGroupChat() {
    patch {
        val update = call.receive<GroupChatUpdate>()
        when {
            !GroupChats.isUserInChat(call.userId, update.chatId) -> call.respond(HttpStatusCode.BadRequest)
            !GroupChats.isAdmin(call.userId, update.chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.update(update)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun Route.leaveGroupChat() {
    delete {
        val chatId = call.parameters["chat_id"]!!.toInt()
        val newAdminUserId = call.parameters["new_admin_user_id"]
        val isAdmin = lazy { GroupChats.isAdmin(call.userId, chatId) }
        when {
            chatId !in GroupChats.read(call.userId).map { it.id } ->
                call.respond(HttpStatusCode.BadRequest, InvalidGroupLeave(InvalidGroupLeaveReason.INVALID_CHAT_ID))
            isAdmin.value && newAdminUserId == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidGroupLeave(InvalidGroupLeaveReason.MISSING_USER_ID))
            isAdmin.value && newAdminUserId !in GroupChatUsers.readUserIdList(chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidGroupLeave(InvalidGroupLeaveReason.INVALID_USER_ID))
            else -> {
                if (isAdmin.value) GroupChats.switchAdmin(chatId, newAdminUserId!!)
                GroupChats.update(GroupChatUpdate(chatId, removedUserIdList = setOf(call.userId)))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}