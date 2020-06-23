package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.db.Users
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.findUserByUsername
import com.neelkamath.omniChat.graphql.EmailAddressTakenException
import com.neelkamath.omniChat.graphql.UsernameNotLowercaseException
import com.neelkamath.omniChat.graphql.UsernameTakenException
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_ACCOUNT_QUERY: String = """
    mutation CreateAccount(${"$"}account: NewAccount!) {
        createAccount(account: ${"$"}account)
    }
"""

private fun operateCreateAccount(account: NewAccount): GraphQlResponse =
    operateQueryOrMutation(CREATE_ACCOUNT_QUERY, variables = mapOf("account" to account))

fun createAccount(account: NewAccount): Boolean = operateCreateAccount(account).data!!["createAccount"] as Boolean

fun errCreateAccount(account: NewAccount): String = operateCreateAccount(account).errors!![0].message

class CreateAccountTest : FunSpec({
    test("Creating an account should save it to the auth system, and the DB") {
        val account = NewAccount("username", "password", "username@example.com")
        createAccount(account)
        with(findUserByUsername(account.username)) {
            username shouldBe account.username
            emailAddress shouldBe account.emailAddress
        }
        Users.count() shouldBe 1
    }

    test("An account with a taken username shouldn't be created") {
        val account = NewAccount("username", "password", "username@example.com")
        createAccount(account)
        errCreateAccount(account) shouldBe UsernameTakenException.message
    }

    test("An account with a taken email shouldn't be created") {
        val address = "username@example.com"
        createAccount(NewAccount("username1", "password", address))
        errCreateAccount(NewAccount("username2", "password", address)) shouldBe EmailAddressTakenException.message
    }

    test("Attempting to create an account with a non-lowercase username should throw an exception") {
        val account = mapOf("username" to "Username", "password" to "password", "emailAddress" to "user@example.com")
        operateQueryOrMutation(
            """
            mutation CreateAccount(${"$"}account: NewAccount!) {
                createAccount(account: ${"$"}account)
            }
            """,
            variables = mapOf("account" to account)
        ).errors!![0].message shouldBe UsernameNotLowercaseException.message
    }
})