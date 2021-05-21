package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidUserId : ReadOnlineStatusResult, CreatePrivateChatResult {
    fun getPlaceholder() = Placeholder
}
