package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object UsernameTaken : UpdateAccountResult, CreateAccountResult {
    fun getPlaceholder() = Placeholder
}
