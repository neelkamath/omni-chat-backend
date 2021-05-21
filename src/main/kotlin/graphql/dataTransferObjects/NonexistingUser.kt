package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class NonexistingUser : RequestTokenSetResult {
    fun getPlaceholder() = Placeholder
}
