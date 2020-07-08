package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.IncorrectPasswordException
import com.neelkamath.omniChat.graphql.NonexistentUserException
import com.neelkamath.omniChat.graphql.UnverifiedEmailAddressException
import com.neelkamath.omniChat.graphql.operations.TOKEN_SET_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val REQUEST_TOKEN_SET_QUERY = """
    query RequestTokenSet(${"$"}login: Login!) {
        requestTokenSet(login: ${"$"}login) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRequestTokenSet(login: Login): GraphQlResponse =
    operateGraphQlQueryOrMutation(REQUEST_TOKEN_SET_QUERY, variables = mapOf("login" to login))

fun requestTokenSet(login: Login): TokenSet {
    val data = operateRequestTokenSet(login).data!!["requestTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errRequestTokenSet(login: Login): String = operateRequestTokenSet(login).errors!![0].message

class RequestTokenSetTest : FunSpec({
    test("The access token should work") {
        val login = createVerifiedUsers(1)[0].login
        val token = requestTokenSet(login).accessToken
        readAccount(token)
    }

    test("A token set shouldn't be created for a nonexistent user") {
        val login = Login(Username("username"), Password("password"))
        errRequestTokenSet(login) shouldBe NonexistentUserException.message
    }

    test("A token set shouldn't be created for a user who hasn't verified their email") {
        val login = Login(Username("username"), Password("password"))
        createUser(NewAccount(login.username, login.password, "username@example.com"))
        errRequestTokenSet(login) shouldBe UnverifiedEmailAddressException.message
    }

    test("A token set shouldn't be created for an incorrect password") {
        val login = createVerifiedUsers(1)[0].login
        val invalidLogin = login.copy(password = Password("incorrect password"))
        errRequestTokenSet(invalidLogin) shouldBe IncorrectPasswordException.message
    }
})