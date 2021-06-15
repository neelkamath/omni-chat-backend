@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class UpdatedPollMessage(
    private val userId: Int,
    private val messageId: Int,
    private val option: MessageText,
    private val vote: Boolean,
) : MessagesSubscription, ChatMessagesSubscription {
    fun getChatId(): Int = Messages.readChatId(messageId)

    fun getMessageId(): Int = messageId

    fun getUserId(): Int = userId

    fun getOption(): MessageText = option

    fun getVote(): Boolean = vote
}
