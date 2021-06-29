package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidUserId : ReadOnlineStatusResult, CreatePrivateChatResult, ReadAccountResult {
    fun getPlaceholder() = Placeholder
}
