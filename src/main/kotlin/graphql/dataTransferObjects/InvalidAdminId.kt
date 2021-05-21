package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidAdminId : CreateGroupChatResult {
    fun getPlaceholder() = Placeholder
}
