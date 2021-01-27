package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.VideoMessages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeVideoMessage(routing: Routing): Unit = with(routing) {
    authenticate {
        route("video-message") {
            getMediaMessage(this) { messageId, _ -> VideoMessages.read(messageId).bytes }
            postMediaMessage(this, { readMultipartMp4() }) { userId, chatId, message, contextMessageId ->
                Messages.createVideoMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}
