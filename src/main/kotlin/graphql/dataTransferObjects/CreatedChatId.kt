package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class CreatedChatId(private val chatId: Int) : CreateGroupChatResult, CreatePrivateChatResult {
    fun getChatId(): Int = chatId
}
