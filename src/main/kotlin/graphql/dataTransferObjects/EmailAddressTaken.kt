package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class EmailAddressTaken : UpdateAccountResult, CreateAccountResult {
    fun getPlaceholder() = Placeholder
}
