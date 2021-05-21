package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidAction : CreateActionMessageResult {
    fun getPlaceholder() = Placeholder
}
