package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidBroadcast : CreateTextMessageResult, ForwardMessageResult {
    fun getPlaceholder() = Placeholder
}
