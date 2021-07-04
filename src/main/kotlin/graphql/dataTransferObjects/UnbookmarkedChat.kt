package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UnbookmarkedChat(private val chatId: Int) : MessagesSubscription {
    fun getChatId(): Int = chatId
}
