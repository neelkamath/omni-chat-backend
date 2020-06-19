package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.graphql.UnregisteredEmailAddressException
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

const val SEND_EMAIL_ADDRESS_VERIFICATION_QUERY: String = """
    mutation SendEmailAddressVerification(${"$"}emailAddress: String!) {
        sendEmailAddressVerification(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateSendEmailAddressVerification(emailAddress: String): GraphQlResponse =
    operateQueryOrMutation(SEND_EMAIL_ADDRESS_VERIFICATION_QUERY, variables = mapOf("emailAddress" to emailAddress))

fun sendEmailAddressVerification(emailAddress: String): Boolean =
    operateSendEmailAddressVerification(emailAddress).data!!["sendEmailAddressVerification"] as Boolean

fun errSendEmailVerification(emailAddress: String): String =
    operateSendEmailAddressVerification(emailAddress).errors!![0].message

class SendEmailAddressVerificationTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A verification email should be sent") {
        val address = "username@example.com"
        val account = NewAccount("username", "password", address)
        createAccount(account)
        sendEmailAddressVerification(address).shouldBeTrue()
    }

    test("Sending a verification email to an unregistered address should throw an exception") {
        errSendEmailVerification("username@example.com") shouldBe UnregisteredEmailAddressException.message
    }
}