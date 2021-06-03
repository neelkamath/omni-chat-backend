package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class DeletedPrivateChat(private val chatId: Int) : ChatsSubscription {
    fun getChatId(): Int = chatId
}
