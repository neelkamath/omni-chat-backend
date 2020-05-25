package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.TokenSet
import com.neelkamath.omniChat.graphql.UnauthorizedException
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.TOKEN_SET_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val REFRESH_TOKEN_SET_QUERY: String = """
    query RefreshTokenSet(${"$"}refreshToken: ID!) {
        refreshTokenSet(refreshToken: ${"$"}refreshToken) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRefreshTokenSet(refreshToken: String): GraphQlResponse =
    operateQueryOrMutation(REFRESH_TOKEN_SET_QUERY, variables = mapOf("refreshToken" to refreshToken))

fun refreshTokenSet(refreshToken: String): TokenSet {
    val data = operateRefreshTokenSet(refreshToken).data!!["refreshTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errRefreshTokenSet(refreshToken: String): String = operateRefreshTokenSet(refreshToken).errors!![0].message

class RefreshTokenSetTest : FunSpec({
    test("A refresh token should issue a new token set") {
        val login = createVerifiedUsers(1)[0].login
        val refreshToken = requestTokenSet(login).refreshToken
        refreshTokenSet(refreshToken)
    }

    test("An invalid refresh token should throw an exception") {
        errRefreshTokenSet(refreshToken = "invalid token") shouldBe UnauthorizedException.message
    }
})