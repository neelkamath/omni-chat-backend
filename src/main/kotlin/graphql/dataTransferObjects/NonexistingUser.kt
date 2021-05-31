package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object NonexistingUser : RequestTokenSetResult {
    fun getPlaceholder() = Placeholder
}
