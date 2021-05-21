package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class IncorrectPassword : RequestTokenSetResult {
    fun getPlaceholder() = Placeholder
}
