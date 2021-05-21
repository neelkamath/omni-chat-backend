package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class NewPollMessage(override val id: Int) : MessagesSubscription, NewMessage {
    fun getPoll(): Poll = Poll(id)
}
