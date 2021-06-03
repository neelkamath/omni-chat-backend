package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidMessageId :
    CreateTextMessageResult,
    SetPollVoteResult,
    ForwardMessageResult,
    CreatePollMessageResult,
    CreatePrivateChatResult,
    CreateActionMessageResult,
    CreateGroupChatInviteMessageResult {

    fun getPlaceholder() = Placeholder
}
