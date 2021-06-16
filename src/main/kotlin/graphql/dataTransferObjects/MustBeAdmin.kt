package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object MustBeAdmin : RemoveGroupChatUsersResult {
    fun getPlaceholder() = Placeholder
}
