package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedGroupChatPic(private val chatId: Int) : ChatsSubscription, GroupChatMetadataSubscription {
    fun getChatId(): Int = chatId
}
