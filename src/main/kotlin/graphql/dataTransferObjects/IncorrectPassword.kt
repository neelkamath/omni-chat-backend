package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object IncorrectPassword : RequestTokenSetResult {
    fun getPlaceholder() = Placeholder
}
