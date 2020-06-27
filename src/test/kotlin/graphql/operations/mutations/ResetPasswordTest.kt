package graphql.operations.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.graphql.UnregisteredEmailAddressException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val RESET_PASSWORD_QUERY = """
    mutation ResetPassword(${"$"}emailAddress: String!) {
        resetPassword(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateResetPassword(emailAddress: String): GraphQlResponse =
    operateGraphQlQueryOrMutation(RESET_PASSWORD_QUERY, variables = mapOf("emailAddress" to emailAddress))

fun resetPassword(emailAddress: String) = operateResetPassword(emailAddress).data!!["resetPassword"]

fun errResetPassword(emailAddress: String): String = operateResetPassword(emailAddress).errors!![0].message

class ResetPasswordTest : FunSpec({
    test("A password reset request should be sent") {
        val address = createSignedInUsers(1)[0].info.emailAddress
        resetPassword(address)
    }

    test("Requesting a password reset for an unregistered address should throw an exception") {
        errResetPassword("username@example.com") shouldBe UnregisteredEmailAddressException.message
    }
})