package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CHAT_QUERY: String = """
    query ReadChat(${"$"}id: Int!) {
        readChat(id: ${"$"}id) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChat(id: Int, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(READ_CHAT_QUERY, variables = mapOf("id" to id), accessToken = accessToken)

fun readChat(id: Int, accessToken: String): Chat {
    val data = operateReadChat(id, accessToken).data!!["readChat"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errReadChat(id: Int, accessToken: String): String = operateReadChat(id, accessToken).errors!![0].message

class ReadChatTest : FunSpec({
    test("The chat should be read") {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        val chat = readChat(chatId, token) as GroupChat
        chat.id shouldBe chatId
    }

    test("Requesting a chat using an invalid ID should return an error") {
        val token = createVerifiedUsers(1)[0].accessToken
        errReadChat(id = 1, accessToken = token) shouldBe InvalidChatIdException.message
    }
})