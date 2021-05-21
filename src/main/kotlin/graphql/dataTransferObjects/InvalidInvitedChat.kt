package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidInvitedChat : CreateGroupChatInviteMessageResult {
    fun getPlaceholder() = Placeholder
}
