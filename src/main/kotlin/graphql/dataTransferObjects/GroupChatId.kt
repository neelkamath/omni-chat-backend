package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class GroupChatId(private val chatId: Int) : GroupChatsSubscription {
    fun getChatId(): Int = chatId
}
