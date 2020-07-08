package com.neelkamath.omniChat.graphql.operations.queries

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.Username
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

const val IS_USERNAME_TAKEN_QUERY = """
    query IsUsernameTaken(${"$"}username: Username!) {
        isUsernameTaken(username: ${"$"}username)
    }
"""

private fun operateIsUsernameTaken(username: Username): GraphQlResponse =
    operateGraphQlQueryOrMutation(IS_USERNAME_TAKEN_QUERY, variables = mapOf("username" to username))

fun isUsernameTaken(username: Username): Boolean = operateIsUsernameTaken(username).data!!["isUsernameTaken"] as Boolean

class IsUsernameTakenTest : FunSpec({
    test("The username shouldn't be taken") { isUsernameTaken(Username("username")).shouldBeFalse() }

    test("The username should be taken") {
        val username = createVerifiedUsers(1)[0].info.username
        isUsernameTaken(username).shouldBeTrue()
    }
})