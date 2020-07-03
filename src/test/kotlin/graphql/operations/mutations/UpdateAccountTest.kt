package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.EmailAddressTakenException
import com.neelkamath.omniChat.graphql.UsernameTakenException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.graphql.operations.queries.requestTokenSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

const val UPDATE_ACCOUNT_QUERY = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update)
    }
"""

private fun operateUpdateAccount(accessToken: String, update: AccountUpdate): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        UPDATE_ACCOUNT_QUERY,
        variables = mapOf("update" to update),
        accessToken = accessToken
    )

fun updateAccount(accessToken: String, update: AccountUpdate): Placeholder {
    val data = operateUpdateAccount(accessToken, update).data!!["updateAccount"] as String
    return objectMapper.convertValue(data)
}

fun errUpdateAccount(accessToken: String, update: AccountUpdate): String =
    operateUpdateAccount(accessToken, update).errors!![0].message

class UpdateAccountTest : FunSpec({
    fun testAccount(accountBeforeUpdate: Account, accountAfterUpdate: AccountUpdate) {
        isUsernameTaken(accountBeforeUpdate.username).shouldBeFalse()
        with(readUserByUsername(accountAfterUpdate.username!!)) {
            username shouldBe accountAfterUpdate.username
            emailAddress shouldBe accountAfterUpdate.emailAddress
            isEmailVerified(id).shouldBeFalse()
            firstName shouldBe accountBeforeUpdate.firstName
            lastName shouldBe accountAfterUpdate.lastName
        }
    }

    test("Only the specified fields should be updated") {
        val user = createSignedInUsers(1)[0]
        val update =
            AccountUpdate(Username("john_roger"), emailAddress = "john.roger@example.com", lastName = "Roger")
        updateAccount(user.accessToken, update)
        testAccount(user.info, update)
    }

    test("The password should be updated") {
        val user = createSignedInUsers(1)[0]
        val newPassword = Password("new password")
        val update = AccountUpdate(password = newPassword)
        updateAccount(user.accessToken, update)
        val login = user.login.copy(password = newPassword)
        requestTokenSet(login)
    }

    test("Updating a username to one already taken shouldn't allow the account to be updated") {
        val (user1, user2) = createSignedInUsers(2)
        errUpdateAccount(user1.accessToken, AccountUpdate(username = user2.info.username)) shouldBe
                UsernameTakenException.message
    }

    test("Updating an email to one already taken shouldn't allow the account to be updated") {
        val (user1, user2) = createSignedInUsers(2)
        errUpdateAccount(user1.accessToken, AccountUpdate(emailAddress = user2.info.emailAddress)) shouldBe
                EmailAddressTakenException.message
    }
})