package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.tables.DocMessages
import com.neelkamath.omniChat.db.tables.Messages
import io.ktor.auth.*
import io.ktor.routing.*

fun routeDocMessage(routing: Routing): Unit = with(routing) {
    authenticate {
        route("doc-message") {
            getMediaMessage(this) { messageId -> DocMessages.read(messageId).bytes }
            postMediaMessage(this, { readMultipartDoc() }) { userId, chatId, message, contextMessageId ->
                Messages.createDocMessage(userId, chatId, message, contextMessageId)
            }
        }
    }
}