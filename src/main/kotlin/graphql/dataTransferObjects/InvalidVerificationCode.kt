package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class InvalidVerificationCode : VerifyEmailAddressResult {
    fun getPlaceholder() = Placeholder
}
