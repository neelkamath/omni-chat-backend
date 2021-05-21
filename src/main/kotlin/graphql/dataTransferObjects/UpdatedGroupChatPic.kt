package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedGroupChatPic(private val chatId: Int) : GroupChatsSubscription {
    fun getChatId(): Int = chatId
}
