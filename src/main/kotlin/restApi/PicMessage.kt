package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.db.isUserInChat
import com.neelkamath.omniChatBackend.db.tables.CaptionedPic
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.PicMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.userId
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import java.io.File

fun routePicMessage(routing: Routing): Unit = with(routing) {
    route("pic-message") {
        authenticate(optional = true) { getPicMessage(this) }
        authenticate { postPicMessage(this) }
    }
}

private fun getPicMessage(route: Route): Unit = with(route) {
    getMediaMessage(this) { messageId, picType ->
        when (picType!!) {
            PicType.ORIGINAL -> PicMessages.read(messageId).pic.original
            PicType.THUMBNAIL -> PicMessages.read(messageId).pic.thumbnail
        }
    }
}

private fun postPicMessage(route: Route): Unit = with(route) {
    post {
        val (pic, chatId, contextMessageId, caption) = try {
            readPicMessageRequest()
        } catch (_: InvalidCaptionException) {
            call.respond(HttpStatusCode.BadRequest, InvalidPicMessage(InvalidPicMessage.Reason.INVALID_CAPTION))
            return@post
        } catch (_: InvalidPicException) {
            call.respond(HttpStatusCode.BadRequest, InvalidPicMessage(InvalidPicMessage.Reason.INVALID_FILE))
            return@post
        }
        when {
            !isUserInChat(call.userId!!, chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidPicMessage(InvalidPicMessage.Reason.USER_NOT_IN_CHAT))

            contextMessageId != null && !Messages.isExisting(contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidPicMessage(InvalidPicMessage.Reason.INVALID_CONTEXT_MESSAGE),
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)

            else -> {
                Messages.createPicMessage(call.userId!!, chatId, CaptionedPic(pic, caption), contextMessageId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private class InvalidCaptionException(message: String? = null) : Exception(message)

private class InvalidPicException(message: String? = null) : Exception(message)

/**
 * @throws [InvalidCaptionException] if the [PicMessageRequest.caption] is invalid.
 * @throws [InvalidPicException] if the [PicMessageRequest.pic] is invalid.
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.readPicMessageRequest(): PicMessageRequest {
    var pic: Pic? = null
    var chatId: Int? = null
    var contextMessageId: Int? = null
    var caption: MessageText? = null
    var isValidCaption = true
    call.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> pic = readPic(part)
            is PartData.FormItem -> {
                when (part.name!!) {
                    "chat-id" -> chatId = part.value.toInt()

                    "context-message-id" -> contextMessageId = part.value.toInt()

                    "caption" -> try {
                        caption = MessageText(part.value)
                    } catch (exception: IllegalArgumentException) {
                        isValidCaption = false
                    }
                }
            }
            else -> throw NoWhenBranchMatchedException()
        }
        part.dispose()
    }
    if (pic == null) throw InvalidPicException()
    if (!isValidCaption) throw InvalidCaptionException()
    return PicMessageRequest(pic!!, chatId!!, contextMessageId, caption)
}

/** Returns `null` if the [Pic] is invalid. */
private fun readPic(part: PartData.FileItem): Pic? {
    val extension = File(part.originalFileName!!).extension
    val bytes = part.streamProvider().use { it.readBytes() }
    return try {
        Pic.build(extension, bytes)
    } catch (exception: IllegalArgumentException) {
        null
    }
}
