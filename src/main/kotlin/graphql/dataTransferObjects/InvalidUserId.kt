package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidUserId : ReadOnlineStatusResult, CreatePrivateChatResult {
    fun getPlaceholder() = Placeholder
}
