package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedGroupChatPic(private val chatId: Int) : GroupChatsSubscription, GroupChatMetadataSubscription {
    fun getChatId(): Int = chatId
}
