package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidInvitedChat : CreateGroupChatInviteMessageResult {
    fun getPlaceholder() = Placeholder
}
