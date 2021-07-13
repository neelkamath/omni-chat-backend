package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.VideoMessages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeVideoMessage(routing: Routing): Unit = with(routing) {
    route("video-message") {
        authenticate(optional = true) {
            getMediaMessage(this) { messageId, _ ->
                val (filename, bytes) = VideoMessages.read(messageId)
                MediaFile(filename, bytes)
            }
        }
        authenticate {
            postMediaMessage(this, { readMultipartMp4() }) { userId, chatId, message, contextMessageId ->
                Messages.createVideoMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}
