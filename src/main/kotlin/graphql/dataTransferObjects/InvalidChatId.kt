package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidChatId :
    CreateTextMessageResult,
    ForwardMessageResult,
    SearchChatMessagesResult,
    ReadChatResult,
    LeaveGroupChatResult,
    CreatePrivateChatResult,
    CreateActionMessageResult,
    CreatePollMessageResult,
    CreateGroupChatInviteMessageResult {

    fun getPlaceholder() = Placeholder
}
