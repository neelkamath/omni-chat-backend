package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidDomain : CreateAccountResult {
    fun getPlaceholder() = Placeholder
}
