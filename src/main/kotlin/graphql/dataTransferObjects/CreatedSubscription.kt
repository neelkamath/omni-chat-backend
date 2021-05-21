package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class CreatedSubscription :
    MessagesSubscription,
    AccountsSubscription,
    GroupChatsSubscription,
    OnlineStatusesSubscription,
    TypingStatusesSubscription {

    fun getPlaceholder() = Placeholder
}
