package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidDomain : CreateAccountResult {
    fun getPlaceholder() = Placeholder
}
