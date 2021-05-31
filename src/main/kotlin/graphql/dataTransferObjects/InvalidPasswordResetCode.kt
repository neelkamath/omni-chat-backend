package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidPasswordResetCode : ResetPasswordResult {
    fun getPlaceholder() = Placeholder
}
