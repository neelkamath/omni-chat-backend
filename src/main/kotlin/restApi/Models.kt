package com.neelkamath.omniChat.restApi

data class InvalidGroupChatPicUpdate(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE }
}

data class InvalidAudioMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE }
}

data class InvalidPicMessage(val reason: Reason) {
    enum class Reason { USER_NOT_IN_CHAT, INVALID_FILE, INVALID_CONTEXT_MESSAGE, INVALID_CAPTION }
}