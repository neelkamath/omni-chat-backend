package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.graphql.routing.MessageText

data class InvalidMediaMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE }
}

data class InvalidPicMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, INVALID_CAPTION }
}

data class PicMessageRequest(
    val pic: Pic,
    val chatId: Int,
    val contextMessageId: Int? = null,
    val caption: MessageText? = null,
)
