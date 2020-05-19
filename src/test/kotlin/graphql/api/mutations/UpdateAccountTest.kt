package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.EmailAddressTakenException
import com.neelkamath.omniChat.graphql.UsernameTakenException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.queries.requestTokenSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

const val UPDATE_ACCOUNT_QUERY: String = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update)
    }
"""

fun errUpdateAccount(update: AccountUpdate, accessToken: String): String =
    operateUpdateAccount(update, accessToken).errors!![0].message

private fun operateUpdateAccount(update: AccountUpdate, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(UPDATE_ACCOUNT_QUERY, variables = mapOf("update" to update), accessToken = accessToken)

fun updateAccount(update: AccountUpdate, accessToken: String): Boolean =
    operateUpdateAccount(update, accessToken).data!!["updateAccount"] as Boolean

class UpdateAccountTest : FunSpec({
    listener(AppListener())

    fun testAccountInfo(account: AccountInfo, updatedAccount: AccountUpdate) {
        isUsernameTaken(account.username).shouldBeFalse()
        with(findUserByUsername(updatedAccount.username!!)) {
            username shouldBe updatedAccount.username
            email shouldBe updatedAccount.emailAddress
            isEmailVerified.shouldBeFalse()
            firstName shouldBe account.firstName
            lastName shouldBe updatedAccount.lastName
        }
    }

    test("Only the specified fields should be updated") {
        val user = createVerifiedUsers(1)[0]
        val update = AccountUpdate(username = "john_roger", emailAddress = "john.roger@example.com", lastName = "Roger")
        updateAccount(update, user.accessToken)
        testAccountInfo(user.info, update)
    }

    test("The password should be updated") {
        val user = createVerifiedUsers(1)[0]
        val newPassword = "new password"
        updateAccount(AccountUpdate(password = newPassword), user.accessToken)
        val login = user.login.copy(password = newPassword)
        requestTokenSet(login) // Successfully requesting a JWT tests the password update.
    }

    test("Updating a username to one already taken shouldn't allow the account to be updated") {
        val (user1, user2) = createVerifiedUsers(2)
        errUpdateAccount(AccountUpdate(username = user2.info.username), user1.accessToken) shouldBe
                UsernameTakenException().message
    }

    test("Updating an email to one already taken shouldn't allow the account to be updated") {
        val (user1, user2) = createVerifiedUsers(2)
        errUpdateAccount(AccountUpdate(emailAddress = user2.info.emailAddress), user1.accessToken) shouldBe
                EmailAddressTakenException().message
    }
})