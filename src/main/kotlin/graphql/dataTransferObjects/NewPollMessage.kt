package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class NewPollMessage(override val id: Int) : MessagesSubscription, NewMessage, ChatMessagesSubscription {
    fun getPoll(): Poll = Poll(id)
}
