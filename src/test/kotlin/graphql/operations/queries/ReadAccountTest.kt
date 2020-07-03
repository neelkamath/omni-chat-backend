package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.Account
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.ACCOUNT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_ACCOUNT_QUERY = """
    query ReadAccount {
        readAccount {
            $ACCOUNT_FRAGMENT
        }
    }
"""

private fun operateReadAccount(accessToken: String): GraphQlResponse =
    operateGraphQlQueryOrMutation(READ_ACCOUNT_QUERY, accessToken = accessToken)

fun readAccount(accessToken: String): Account {
    val data = operateReadAccount(accessToken).data!!["readAccount"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class ReadAccountTest : FunSpec({
    test("The user's account info should be returned") {
        val user = createSignedInUsers(1)[0]
        readAccount(user.accessToken) shouldBe user.info
    }
})