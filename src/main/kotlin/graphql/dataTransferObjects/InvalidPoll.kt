package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidPoll : CreatePollMessageResult {
    fun getPlaceholder() = Placeholder
}
