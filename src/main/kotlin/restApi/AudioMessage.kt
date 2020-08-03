package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.tables.AudioMessages
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.Mp3
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
        val contextMessageId = call.parameters["context-message-id"]?.toInt()
        val audio = readMp3()
        when {
            !isUserInChat(call.userId!!, chatId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidAudioMessage(InvalidAudioMessage.Reason.USER_NOT_IN_CHAT)
            )

            audio == null -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidAudioMessage(InvalidAudioMessage.Reason.INVALID_FILE)
            )

            contextMessageId != null && !Messages.exists(contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidAudioMessage(InvalidAudioMessage.Reason.INVALID_CONTEXT_MESSAGE)
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)

            else -> {
                Messages.create(call.userId!!, chatId, audio, contextMessageId)
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
                    if (File(part.originalFileName!!).extension != "mp3" || bytes.size > Mp3.MAX_BYTES) null
                    else Mp3(bytes)
            }
            else -> throw NoWhenBranchMatchedException()
        }
        part.dispose()
    }
    return audio
}