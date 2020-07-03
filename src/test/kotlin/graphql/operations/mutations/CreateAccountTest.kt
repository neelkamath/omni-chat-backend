package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.count
import com.neelkamath.omniChat.graphql.EmailAddressTakenException
import com.neelkamath.omniChat.graphql.UsernameTakenException
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_ACCOUNT_QUERY = """
    mutation CreateAccount(${"$"}account: NewAccount!) {
        createAccount(account: ${"$"}account)
    }
"""

private fun operateCreateAccount(account: NewAccount): GraphQlResponse =
    operateGraphQlQueryOrMutation(CREATE_ACCOUNT_QUERY, variables = mapOf("account" to account))

fun createAccount(account: NewAccount): Placeholder {
    val data = operateCreateAccount(account).data!!["createAccount"] as String
    return objectMapper.convertValue(data)
}

fun errCreateAccount(account: NewAccount): String = operateCreateAccount(account).errors!![0].message

class CreateAccountTest : FunSpec({
    test("Creating an account should save it to the auth system, and the DB") {
        val account = NewAccount(Username("username"), Password("password"), "username@example.com")
        createAccount(account)
        with(readUserByUsername(account.username)) {
            username shouldBe account.username
            emailAddress shouldBe account.emailAddress
        }
        Users.count() shouldBe 1
    }

    test("An account with a taken username shouldn't be created") {
        val account = NewAccount(Username("username"), Password("password"), "username@example.com")
        createAccount(account)
        errCreateAccount(account) shouldBe UsernameTakenException.message
    }

    test("An account with a taken email shouldn't be created") {
        val address = "username@example.com"
        val account = NewAccount(Username("username1"), Password("password"), address)
        createAccount(account)
        val duplicateAccount = NewAccount(Username("username2"), Password("password"), address)
        errCreateAccount(duplicateAccount) shouldBe EmailAddressTakenException.message
    }
})