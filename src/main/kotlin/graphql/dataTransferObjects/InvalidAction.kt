package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidAction : CreateActionMessageResult {
    fun getPlaceholder() = Placeholder
}
