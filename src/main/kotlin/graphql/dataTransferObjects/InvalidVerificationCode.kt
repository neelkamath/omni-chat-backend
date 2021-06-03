package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object InvalidVerificationCode : VerifyEmailAddressResult {
    fun getPlaceholder() = Placeholder
}
