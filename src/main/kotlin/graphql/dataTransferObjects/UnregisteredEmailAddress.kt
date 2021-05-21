package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class UnregisteredEmailAddress :
    VerifyEmailAddressResult,
    ResetPasswordResult,
    EmailEmailAddressVerificationResult {

    fun getPlaceholder() = Placeholder
}
