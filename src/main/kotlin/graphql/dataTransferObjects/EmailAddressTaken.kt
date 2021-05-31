package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object EmailAddressTaken : UpdateAccountResult, CreateAccountResult {
    fun getPlaceholder() = Placeholder
}
