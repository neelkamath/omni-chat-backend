package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.UnregisteredEmailAddressException
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SEND_EMAIL_ADDRESS_VERIFICATION_QUERY = """
    mutation SendEmailAddressVerification(${"$"}emailAddress: String!) {
        sendEmailAddressVerification(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateSendEmailAddressVerification(emailAddress: String): GraphQlResponse = operateGraphQlQueryOrMutation(
    SEND_EMAIL_ADDRESS_VERIFICATION_QUERY,
    variables = mapOf("emailAddress" to emailAddress)
)

fun sendEmailAddressVerification(emailAddress: String): Placeholder {
    val data = operateSendEmailAddressVerification(emailAddress).data!!["sendEmailAddressVerification"] as String
    return objectMapper.convertValue(data)
}

fun errSendEmailVerification(emailAddress: String): String =
    operateSendEmailAddressVerification(emailAddress).errors!![0].message

class SendEmailAddressVerificationTest : FunSpec({
    test("A verification email should be sent") {
        val address = "username@example.com"
        val account = NewAccount(Username("username"), Password("password"), address)
        createAccount(account)
        sendEmailAddressVerification(address)
    }

    test("Sending a verification email to an unregistered address should throw an exception") {
        errSendEmailVerification("username@example.com") shouldBe UnregisteredEmailAddressException.message
    }
})