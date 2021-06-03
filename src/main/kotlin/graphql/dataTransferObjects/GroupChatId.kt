package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class GroupChatId(private val chatId: Int) : ChatsSubscription {
    fun getChatId(): Int = chatId
}
