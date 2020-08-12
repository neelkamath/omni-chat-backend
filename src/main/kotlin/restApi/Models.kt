package com.neelkamath.omniChat.restApi

data class InvalidMediaMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE }
}

data class InvalidPicMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, INVALID_CAPTION }
}