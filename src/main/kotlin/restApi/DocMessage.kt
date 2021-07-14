package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.tables.DocMessages
import com.neelkamath.omniChatBackend.db.tables.Messages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeDocMessage(routing: Routing): Unit = with(routing) {
    route("doc-message") {
        authenticate(optional = true) {
            getMediaMessage(this, FileDisposition.ATTACHMENT) { messageId, _ ->
                val (filename, bytes) = DocMessages.read(messageId)
                MediaFile(filename, bytes)
            }
        }
        authenticate {
            postMediaMessage(this, { readMultipartDoc() }) { userId, chatId, message, contextMessageId ->
                Messages.createDocMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}
