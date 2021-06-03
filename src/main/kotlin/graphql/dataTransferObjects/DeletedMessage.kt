package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class DeletedMessage(private val chatId: Int, private val messageId: Int) :
    MessagesSubscription,
    ChatMessagesSubscription {

    fun getChatId(): Int = chatId

    fun getMessageId(): Int = messageId
}
