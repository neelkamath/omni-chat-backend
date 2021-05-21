package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class PollMessage(override val id: Int) : Message {
    fun getPoll(): Poll = Poll(id)
}
