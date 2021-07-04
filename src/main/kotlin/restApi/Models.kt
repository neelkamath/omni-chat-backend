package com.neelkamath.omniChatBackend.restApi

import com.neelkamath.omniChatBackend.db.Pic
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

data class InvalidMediaMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, MUST_BE_ADMIN }
}

data class InvalidPicMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, INVALID_CAPTION, MUST_BE_ADMIN }
}

data class PicMessageRequest(
    val pic: Pic,
    val chatId: Int,
    val contextMessageId: Int? = null,
    val caption: MessageText? = null,
)
