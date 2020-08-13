package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.AudioMessages
import com.neelkamath.omniChat.db.tables.Messages
import io.ktor.auth.authenticate
import io.ktor.routing.Routing
import io.ktor.routing.route

fun routeAudioMessage(routing: Routing): Unit = with(routing) {
    authenticate {
        route("audio-message") {
            getMediaMessage(this) { messageId -> AudioMessages.read(messageId).bytes }
            postMediaMessage(this, { readMultipartMp3() }) { userId, chatId, message, contextMessageId ->
                Messages.createAudioMessage(userId, chatId, message, contextMessageId, isForwarded = false)
            }
        }
    }
}