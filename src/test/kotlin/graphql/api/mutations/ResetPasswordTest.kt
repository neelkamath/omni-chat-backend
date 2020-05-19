package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.graphql.UnregisteredEmailAddressException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

const val RESET_PASSWORD_QUERY: String = """
    mutation ResetPassword(${"$"}emailAddress: String!) {
        resetPassword(emailAddress: ${"$"}emailAddress)
    }
"""

fun errResetPassword(emailAddress: String): String = operateResetPassword(emailAddress).errors!![0].message

private fun operateResetPassword(emailAddress: String): GraphQlResponse =
    operateQueryOrMutation(RESET_PASSWORD_QUERY, variables = mapOf("emailAddress" to emailAddress))

fun resetPassword(emailAddress: String): Boolean = operateResetPassword(emailAddress).data!!["resetPassword"] as Boolean

class ResetPasswordTest : FunSpec({
    listener(AppListener())

    test("A password reset request should be sent") {
        val address = createVerifiedUsers(1)[0].info.emailAddress
        resetPassword(address).shouldBeTrue()
    }

    test("Requesting a password reset for an unregistered address should throw an exception") {
        errResetPassword("username@example.com") shouldBe UnregisteredEmailAddressException().message
    }
})