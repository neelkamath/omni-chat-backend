package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.tables.CaptionedPic
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.PicMessages
import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.routing.post
import io.ktor.routing.route

fun routePicMessage(routing: Routing): Unit = with(routing) {
    authenticate {
        route("pic-message") {
            getMediaMessage(this) { messageId -> PicMessages.read(messageId).pic.bytes }
            postPicMessage(this)
        }
    }
}

private fun postPicMessage(route: Route): Unit = with(route) {
    post {
        val chatId = call.parameters["chat-id"]!!.toInt()
        val contextMessageId = call.parameters["context-message-id"]?.toInt()
        val caption = try {
            call.parameters["caption"]?.let(::MessageText)
        } catch (_: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, InvalidPicMessage(InvalidPicMessage.Reason.INVALID_CAPTION))
            return@post
        }
        val pic = readMultipartPic()
        when {
            !isUserInChat(call.userId!!, chatId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidPicMessage(InvalidPicMessage.Reason.USER_NOT_IN_CHAT)
            )

            pic == null -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidPicMessage(InvalidPicMessage.Reason.INVALID_FILE)
            )

            contextMessageId != null && !Messages.exists(contextMessageId) -> call.respond(
                HttpStatusCode.BadRequest,
                InvalidPicMessage(InvalidPicMessage.Reason.INVALID_CONTEXT_MESSAGE)
            )

            Messages.isInvalidBroadcast(call.userId!!, chatId) -> call.respond(HttpStatusCode.Unauthorized)

            else -> {
                Messages.create(call.userId!!, chatId, CaptionedPic(pic, caption), contextMessageId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}