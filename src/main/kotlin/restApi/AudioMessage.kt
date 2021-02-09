package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.AudioMessages
import com.neelkamath.omniChat.db.tables.Messages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeAudioMessage(routing: Routing): Unit = with(routing) {
    route("audio-message") {
        authenticate(optional = true) {
            getMediaMessage(this) { messageId, _ -> AudioMessages.read(messageId).bytes }
        }
        authenticate {
            postMediaMessage(this, { readMultipartAudio() }) { userId, chatId, message, contextMessageId ->
                Messages.createAudioMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}
