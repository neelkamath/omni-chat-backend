package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.DocMessages
import com.neelkamath.omniChat.db.tables.Messages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeDocMessage(routing: Routing): Unit = with(routing) {
    route("doc-message") {
        authenticate(optional = true) {
            getMediaMessage(this) { messageId, _ -> DocMessages.read(messageId).bytes }
        }
        authenticate {
            postMediaMessage(this, { readMultipartDoc() }) { userId, chatId, message, contextMessageId ->
                Messages.createDocMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}
