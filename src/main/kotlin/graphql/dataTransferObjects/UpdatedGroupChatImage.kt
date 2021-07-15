package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedGroupChatImage(private val chatId: Int) : ChatsSubscription, GroupChatMetadataSubscription {
    fun getChatId(): Int = chatId
}
