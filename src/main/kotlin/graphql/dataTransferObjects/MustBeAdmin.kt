package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object MustBeAdmin :
    RemoveGroupChatUsersResult,
    CreatePollMessageResult,
    CreateTextMessageResult,
    CreateGroupChatInviteMessageResult,
    ForwardMessageResult {

    fun getPlaceholder() = Placeholder
}
