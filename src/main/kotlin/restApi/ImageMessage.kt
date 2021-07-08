package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.ProcessedImage
import com.neelkamath.omniChatBackend.db.isUserInChat
import com.neelkamath.omniChatBackend.db.tables.CaptionedImage
import com.neelkamath.omniChatBackend.db.tables.ImageMessages
import com.neelkamath.omniChatBackend.db.tables.Messages
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

fun routeImageMessage(routing: Routing): Unit = with(routing) {
    route("image-message") {
        authenticate(optional = true) { getImageMessage(this) }
        authenticate { postImageMessage(this) }
    }
}

private fun getImageMessage(route: Route): Unit = with(route) {
    getMediaMessage(this) { messageId, imageType -> ImageMessages.readImage(messageId, imageType!!) }
}

private fun postImageMessage(route: Route): Unit = with(route) {
    post {
        val (image, chatId, contextMessageId, caption) = try {
            readImageMessageRequest()
        } catch (_: InvalidCaptionException) {
            call.respond(HttpStatusCode.BadRequest, InvalidImageMessage(InvalidImageMessage.Reason.INVALID_CAPTION))
            return@post
        } catch (_: InvalidImageException) {
            call.respond(HttpStatusCode.BadRequest, InvalidImageMessage(InvalidImageMessage.Reason.INVALID_FILE))
            return@post
        }
        when {
            !isUserInChat(call.userId!!, chatId) ->
                call.respond(
                    HttpStatusCode.BadRequest,
                    InvalidImageMessage(InvalidImageMessage.Reason.USER_NOT_IN_CHAT)
                )

            !Messages.isValidContext(call.userId!!, chatId, contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidImageMessage(InvalidImageMessage.Reason.INVALID_CONTEXT_MESSAGE),
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) ->
                call.respond(HttpStatusCode.BadRequest, InvalidImageMessage(InvalidImageMessage.Reason.MUST_BE_ADMIN))

            else -> {
                Messages.createImageMessage(call.userId!!, chatId, CaptionedImage(image, caption), contextMessageId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private class InvalidCaptionException(message: String? = null) : Exception(message)

private class InvalidImageException(message: String? = null) : Exception(message)

/**
 * @throws InvalidCaptionException if the [ImageMessageRequest.caption] is invalid.
 * @throws InvalidImageException if the [ImageMessageRequest.image] is invalid.
 */
private suspend fun PipelineContext<Unit, ApplicationCall>.readImageMessageRequest(): ImageMessageRequest {
    var image: ProcessedImage? = null
    var chatId: Int? = null
    var contextMessageId: Int? = null
    var caption: MessageText? = null
    var isValidCaption = true
    call.receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FileItem -> image = readImage(part)
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
    if (image == null) throw InvalidImageException()
    if (!isValidCaption) throw InvalidCaptionException()
    return ImageMessageRequest(image!!, chatId!!, contextMessageId, caption)
}

/** Returns `null` if the [ProcessedImage] is invalid. */
private fun readImage(part: PartData.FileItem): ProcessedImage? {
    val extension = File(part.originalFileName!!).extension
    val bytes = part.streamProvider().use { it.readBytes() }
    return try {
        ProcessedImage.build(extension, bytes)
    } catch (exception: IllegalArgumentException) {
        null
    }
}
