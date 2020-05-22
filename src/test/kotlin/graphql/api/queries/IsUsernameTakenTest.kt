package com.neelkamath.omniChat.test.graphql.api.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

const val IS_USERNAME_TAKEN_QUERY: String = """
    query IsUsernameTaken(${"$"}username: String!) {
        isUsernameTaken(username: ${"$"}username)
    }
"""

private fun operateIsUsernameTaken(username: String): GraphQlResponse =
    operateQueryOrMutation(IS_USERNAME_TAKEN_QUERY, variables = mapOf("username" to username))

fun isUsernameTaken(username: String): Boolean = operateIsUsernameTaken(username).data!!["isUsernameTaken"] as Boolean

class IsUsernameTakenTest : FunSpec({
    listener(AppListener())

    test("The username shouldn't be taken") { isUsernameTaken("username").shouldBeFalse() }

    test("The username should be taken") {
        val username = createVerifiedUsers(1)[0].info.username
        isUsernameTaken(username).shouldBeTrue()
    }
})