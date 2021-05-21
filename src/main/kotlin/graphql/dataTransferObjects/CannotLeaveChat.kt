package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class CannotLeaveChat : LeaveGroupChatResult {
    fun getPlaceholder() = Placeholder
}
