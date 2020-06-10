package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.TokenSet
import com.neelkamath.omniChat.graphql.UnauthorizedException
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.graphql.api.buildTokenSetFragment
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

fun buildRefreshTokenSetQuery(): String = """
    query RefreshTokenSet(${"$"}refreshToken: ID!) {
        refreshTokenSet(refreshToken: ${"$"}refreshToken) {
            ${buildTokenSetFragment()}
        }
    }
"""

private fun operateRefreshTokenSet(refreshToken: String): GraphQlResponse =
    operateQueryOrMutation(buildRefreshTokenSetQuery(), variables = mapOf("refreshToken" to refreshToken))

fun refreshTokenSet(refreshToken: String): TokenSet {
    val data = operateRefreshTokenSet(refreshToken).data!!["refreshTokenSet"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errRefreshTokenSet(refreshToken: String): String = operateRefreshTokenSet(refreshToken).errors!![0].message

class RefreshTokenSetTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A refresh token should issue a new token set") {
        val login = createSignedInUsers(1)[0].login
        val refreshToken = requestTokenSet(login).refreshToken
        refreshTokenSet(refreshToken)
    }

    test("An invalid refresh token should throw an exception") {
        errRefreshTokenSet(refreshToken = "invalid token") shouldBe UnauthorizedException.message
    }
}