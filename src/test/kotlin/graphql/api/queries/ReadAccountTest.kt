package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.AccountInfo
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.ACCOUNT_INFO_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_ACCOUNT_QUERY: String = """
    query ReadAccount {
        readAccount {
            $ACCOUNT_INFO_FRAGMENT
        }
    }
"""

private fun operateReadAccount(accessToken: String): GraphQlResponse =
    operateQueryOrMutation(READ_ACCOUNT_QUERY, accessToken = accessToken)

fun readAccount(accessToken: String): AccountInfo {
    val data = operateReadAccount(accessToken).data!!["readAccount"] as Map<*, *>
    return objectMapper.convertValue(data)
}

class ReadAccountTest : FunSpec({
    test("The user's account info should be returned") {
        val user = createVerifiedUsers(1)[0]
        readAccount(user.accessToken) shouldBe user.info
    }
})