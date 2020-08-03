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
import io.ktor.routing.*

fun routePicMessage(routing: Routing): Unit = with(routing) {
    authenticate {
        route("pic-message") {
            getPicMessage(this)
            postPicMessage(this)
        }
    }
}

private fun getPicMessage(route: Route): Unit = with(route) {
    get {
        val messageId = call.parameters["message-id"]!!.toInt()
        if (!Messages.isVisible(call.userId!!, messageId)) call.respond(HttpStatusCode.BadRequest)
        else call.respond(PicMessages.read(messageId).pic.bytes)
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
        val pic = readPic()
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