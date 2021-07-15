package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.ProcessedImage
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

data class InvalidMediaMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, MUST_BE_ADMIN }
}

data class InvalidImageMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, INVALID_CAPTION, MUST_BE_ADMIN }
}

data class ImageMessageRequest(
    val image: ProcessedImage,
    val chatId: Int,
    val contextMessageId: Int? = null,
    val caption: MessageText? = null,
)
