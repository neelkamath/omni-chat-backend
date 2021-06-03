package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object UnregisteredEmailAddress :
    VerifyEmailAddressResult,
    ResetPasswordResult,
    EmailEmailAddressVerificationResult {

    fun getPlaceholder() = Placeholder
}
