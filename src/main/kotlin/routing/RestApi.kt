package com.neelkamath.omniChat.routing

import com.neelkamath.omniChat.InvalidFileUpload
import com.neelkamath.omniChat.db.isUserInChat
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

fun routeAudioMessage(routing: Routing): Unit = with(routing) {
    authenticate {
        route("audio-message") {
            getAudioMessage(this)
            postAudioMessage(this)
        }
    }
}

private fun getAudioMessage(route: Route): Unit = with(route) {
    get {
        val messageId = call.parameters["message-id"]!!.toInt()
        if (Messages.isVisible(call.userId!!, messageId)) call.respondBytes(AudioMessages.read(messageId).bytes)
        else call.respond(HttpStatusCode.BadRequest)
    }
}

private fun postAudioMessage(route: Route): Unit = with(route) {
    post {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val audio = readMp3()
        when {
            !isUserInChat(call.userId!!, chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidFileUpload(InvalidFileUpload.Reason.INVALID_CHAT_ID))
            audio == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidFileUpload(InvalidFileUpload.Reason.INVALID_FILE))
            Messages.isInvalidBroadcast(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                Messages.create(call.userId!!, chatId, audio, call.parameters["context-message-id"]?.toInt())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Mp3] is invalid.
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.readMp3(): Mp3? {
    var audio: Mp3? = null
    call.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val bytes = part.streamProvider().use { it.readBytes() }
                audio =
                    if (File(part.originalFileName!!).extension != "mp3" || bytes.size > AudioMessages.MAX_AUDIO_BYTES)
                        null
                    else Mp3(bytes)
            }
            else -> throw NoWhenBranchMatchedException()
        }
        part.dispose()
    }
    return audio
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
        val pic = readPic()
        if (pic == null) {
            call.respond(HttpStatusCode.BadRequest)
            return@patch
        }
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
        val chatId = call.parameters["chat-id"]!!.toInt()
        val pic = readPic()
        when {
            pic == null ->
                call.respond(HttpStatusCode.BadRequest, InvalidFileUpload(InvalidFileUpload.Reason.INVALID_FILE))
            chatId !in GroupChatUsers.readChatIdList(call.userId!!) ->
                call.respond(HttpStatusCode.BadRequest, InvalidFileUpload(InvalidFileUpload.Reason.INVALID_CHAT_ID))
            !GroupChatUsers.isAdmin(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)
            else -> {
                GroupChats.updatePic(chatId, pic)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

/**
 * Receives a multipart request with only one part, where the part is a [PartData.FileItem]. `null` will be returned if
 * the [Pic] is invalid.
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
                    null
                }
            }
            else -> throw NoWhenBranchMatchedException()
        }
        part.dispose()
    }
    return pic
}