package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.TokenSet
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.graphql.operations.requestGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.shouldHaveUnauthorizedStatus
import graphql.operations.TOKEN_SET_FRAGMENT
import graphql.operations.queries.requestTokenSet
import io.kotest.core.spec.style.FunSpec

const val REFRESH_TOKEN_SET_QUERY = """
    query RefreshTokenSet(${"$"}refreshToken: ID!) {
        refreshTokenSet(refreshToken: ${"$"}refreshToken) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRefreshTokenSet(refreshToken: String): GraphQlResponse =
    operateGraphQlQueryOrMutation(REFRESH_TOKEN_SET_QUERY, variables = mapOf("refreshToken" to refreshToken))

fun refreshTokenSet(refreshToken: String): TokenSet {
    val data = operateRefreshTokenSet(refreshToken).data!!["refreshTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class RefreshTokenSetTest : FunSpec({
    test("A refresh token should issue a new token set") {
        val login = createSignedInUsers(1)[0].login
        val refreshToken = requestTokenSet(login).refreshToken
        refreshTokenSet(refreshToken)
    }

    test("An invalid refresh token should return an authorization error") {
        val variables = mapOf("refreshToken" to "invalid token")
        requestGraphQlQueryOrMutation(REFRESH_TOKEN_SET_QUERY, variables).shouldHaveUnauthorizedStatus()
    }
})