package com.neelkamath.omniChat.routing

import com.neelkamath.omniChat.InvalidGroupChatPic
import com.neelkamath.omniChat.InvalidGroupChatPicReason
import com.neelkamath.omniChat.db.tables.Chats
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.userId
import com.neelkamath.omniChat.userIdExists
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext

fun routeHealthCheck(routing: Routing): Unit = with(routing) {
    get("health-check") { call.respond(HttpStatusCode.NoContent) }
}

fun routeProfilePic(routing: Routing): Unit = with(routing) {
    route("profile-pic") {
        getProfilePic(this)
        authenticate { patchProfilePic(this) }
    }
}

private fun getProfilePic(route: Route): Unit = with(route) {
    get {
        val userId = call.parameters["user-id"]!!
        if (!userIdExists(userId)) call.respond(HttpStatusCode.BadRequest)
        else call.respond(Users.readPic(userId) ?: HttpStatusCode.NoContent)
    }
}

private fun patchProfilePic(route: Route): Unit = with(route) {
    patch {
        val pic = readBytes()
        if (pic.size > Users.MAX_PIC_BYTES) call.respond(HttpStatusCode.BadRequest)
        else {
            Users.updatePic(call.userId!!, pic)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

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
        else call.respond(GroupChats.readPic(chatId) ?: HttpStatusCode.NoContent)
    }
}

private fun patchGroupChatPic(route: Route): Unit = with(route) {
    patch {
        val pic = readBytes()
        val chatId = call.parameters["chat-id"]!!.toInt()
        when {
            !Chats.exists(chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidGroupChatPic(InvalidGroupChatPicReason.NONEXISTENT_CHAT))
            !GroupChats.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            pic.size > GroupChats.MAX_PIC_BYTES ->
                call.respond(HttpStatusCode.BadRequest, InvalidGroupChatPic(InvalidGroupChatPicReason.PIC_TOO_BIG))
            else -> {
                GroupChats.updatePic(chatId, pic)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/** Call to receive the bytes of a multipart request with only one part, where the part is a [PartData.FileItem]. */
private suspend fun PipelineContext<Unit, ApplicationCall>.readBytes(): ByteArray {
    val multipart = call.receiveMultipart()
    val part = multipart.readPart()!! as PartData.FileItem
    val image = part.streamProvider().use { it.readBytes() }
    part.dispose()
    multipart.readPart() // Workaround for multipart requests hanging (https://github.com/ktorio/ktor/issues/1936).
    return image
}