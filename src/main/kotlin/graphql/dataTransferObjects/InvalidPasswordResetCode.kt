package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidPasswordResetCode : ResetPasswordResult {
    fun getPlaceholder() = Placeholder
}
