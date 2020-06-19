package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.IncorrectPasswordException
import com.neelkamath.omniChat.graphql.NonexistentUserException
import com.neelkamath.omniChat.graphql.UnverifiedEmailAddressException
import com.neelkamath.omniChat.graphql.api.TOKEN_SET_FRAGMENT
import com.neelkamath.omniChat.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val REQUEST_TOKEN_SET_QUERY: String = """
    query RequestTokenSet(${"$"}login: Login!) {
        requestTokenSet(login: ${"$"}login) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRequestTokenSet(login: Login): GraphQlResponse =
    operateQueryOrMutation(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login))

fun requestTokenSet(login: Login): TokenSet {
    val data = operateRequestTokenSet(login).data!!["requestTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errRequestTokenSet(login: Login): String = operateRequestTokenSet(login).errors!![0].message

class RequestTokenSetTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("The access token should work") {
        val login = createSignedInUsers(1)[0].login
        val token = requestTokenSet(login).accessToken
        readAccount(token)
    }

    test("A token set shouldn't be created for a nonexistent user") {
        errRequestTokenSet(Login("username", "password")) shouldBe NonexistentUserException.message
    }

    test("A token set shouldn't be created for a user who hasn't verified their email") {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        errRequestTokenSet(login) shouldBe UnverifiedEmailAddressException.message
    }

    test("A token set shouldn't be created for an incorrect password") {
        val login = createSignedInUsers(1)[0].login
        errRequestTokenSet(login.copy(password = "incorrect password")) shouldBe IncorrectPasswordException.message
    }
}