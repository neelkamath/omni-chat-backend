package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UnstarredChat(private val chatId: Int) : MessagesSubscription {
    fun getChatId(): Int = chatId
}
