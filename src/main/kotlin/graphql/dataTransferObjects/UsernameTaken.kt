package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class UsernameTaken : UpdateAccountResult, CreateAccountResult {
    fun getPlaceholder() = Placeholder
}
