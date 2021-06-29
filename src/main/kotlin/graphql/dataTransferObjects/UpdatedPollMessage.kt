@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class UpdatedPollMessage(private val messageId: Int) : MessagesSubscription, ChatMessagesSubscription {
    fun getChatId(): Int = Messages.readChatId(messageId)

    fun getMessageId(): Int = messageId

    fun getPoll(): Poll = Poll(messageId)
}
