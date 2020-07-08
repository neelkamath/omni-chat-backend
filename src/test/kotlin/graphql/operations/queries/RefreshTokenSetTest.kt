package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.operations.TOKEN_SET_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.graphql.operations.requestGraphQlQueryOrMutation
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
        val userId = createVerifiedUsers(1)[0].info.id
        val refreshToken = buildAuthToken(userId).refreshToken
        refreshTokenSet(refreshToken)
    }

    test("An invalid refresh token should return an authorization error") {
        val variables = mapOf("refreshToken" to "invalid token")
        requestGraphQlQueryOrMutation(REFRESH_TOKEN_SET_QUERY, variables).shouldHaveUnauthorizedStatus()
    }
})