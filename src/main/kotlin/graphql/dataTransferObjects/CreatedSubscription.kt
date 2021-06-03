package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object CreatedSubscription :
    ChatOnlineStatusesSubscription,
    ChatTypingStatusesSubscription,
    ChatAccountsSubscription,
    GroupChatMetadataSubscription,
    ChatMessagesSubscription,
    MessagesSubscription,
    AccountsSubscription,
    ChatsSubscription,
    OnlineStatusesSubscription,
    TypingStatusesSubscription {

    fun getPlaceholder() = Placeholder
}
