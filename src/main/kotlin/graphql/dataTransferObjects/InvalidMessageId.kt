package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidMessageId :
    CreateTextMessageResult,
    SetPollVoteResult,
    ForwardMessageResult,
    CreatePollMessageResult,
    CreatePrivateChatResult,
    CreateActionMessageResult,
    CreateGroupChatInviteMessageResult {

    fun getPlaceholder() = Placeholder
}
