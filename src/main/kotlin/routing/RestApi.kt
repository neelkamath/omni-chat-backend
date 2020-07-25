package com.neelkamath.omniChat.routing

import com.neelkamath.omniChat.InvalidGroupChatPic
import com.neelkamath.omniChat.InvalidGroupChatPicReason
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.userId
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import java.io.File

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
        val userId = call.parameters["user-id"]!!.toInt()
        if (!Users.exists(userId)) call.respond(HttpStatusCode.BadRequest)
        else {
            val pic = Users.read(userId).pic
            if (pic == null) call.respond(HttpStatusCode.NoContent) else call.respondBytes(pic.bytes)
        }
    }
}

private fun patchProfilePic(route: Route): Unit = with(route) {
    patch {
        val pic = readPic() ?: return@patch
        Users.updatePic(call.userId!!, pic)
        call.respond(HttpStatusCode.NoContent)
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
        else {
            val pic = GroupChats.readPic(chatId)
            if (pic == null) call.respond(HttpStatusCode.NoContent) else call.respondBytes(pic.bytes)
        }
    }
}

private fun patchGroupChatPic(route: Route): Unit = with(route) {
    patch {
        val pic = readPic() ?: return@patch
        val chatId = call.parameters["chat-id"]!!.toInt()
        when {
            !Chats.exists(chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidGroupChatPic(InvalidGroupChatPicReason.NONEXISTENT_CHAT))
            !GroupChatUsers.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.updatePic(chatId, pic)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. If the [Pic] is invalid,
 * an [HttpStatusCode.BadRequest] will be sent, and `null` will be returned.
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.readPic(): Pic? {
    var pic: Pic? = null
    call.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val bytes = part.streamProvider().use { it.readBytes() }
                pic = try {
                    Pic.build(bytes, File(part.originalFileName!!).extension)
                } catch (_: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@forEachPart
                }
                part.dispose()
            }
            else -> throw NoWhenBranchMatchedException()
        }
    }
    return pic
}