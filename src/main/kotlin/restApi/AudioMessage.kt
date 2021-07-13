package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.tables.AudioMessages
import com.neelkamath.omniChatBackend.db.tables.Messages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeAudioMessage(routing: Routing): Unit = with(routing) {
    route("audio-message") {
        authenticate(optional = true) {
            getMediaMessage(this) { messageId, _ ->
                val (filename, bytes) = AudioMessages.read(messageId)
                MediaFile(filename, bytes)
            }
        }
        authenticate {
            postMediaMessage(this, { readMultipartAudio() }) { userId, chatId, message, contextMessageId ->
                Messages.createAudioMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}
