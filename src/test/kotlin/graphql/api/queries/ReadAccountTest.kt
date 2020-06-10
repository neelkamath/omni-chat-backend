package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.AccountInfo
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.graphql.api.buildAccountInfoFragment
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

fun buildReadAccountQuery(): String = """
    query ReadAccount {
        readAccount {
            ${buildAccountInfoFragment()}
        }
    }
"""

private fun operateReadAccount(accessToken: String): GraphQlResponse =
    operateQueryOrMutation(buildReadAccountQuery(), accessToken = accessToken)

fun readAccount(accessToken: String): AccountInfo {
    val data = operateReadAccount(accessToken).data!!["readAccount"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class ReadAccountTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("The user's account info should be returned") {
        val user = createSignedInUsers(1)[0]
        readAccount(user.accessToken) shouldBe user.info
    }
}