package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object CannotLeaveChat : LeaveGroupChatResult {
    fun getPlaceholder() = Placeholder
}
