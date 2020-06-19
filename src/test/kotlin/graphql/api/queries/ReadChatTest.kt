package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.api.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.api.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CHAT_QUERY: String = """
    query ReadChat(${"$"}id: Int!, ${"$"}last: Int, ${"$"}before: Cursor) {
        readChat(id: ${"$"}id) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChat(accessToken: String, id: Int, pagination: BackwardPagination? = null): GraphQlResponse =
    operateQueryOrMutation(
        READ_CHAT_QUERY,
        variables = mapOf("id" to id, "last" to pagination?.last, "before" to pagination?.before?.toString()),
        accessToken = accessToken
    )

fun readChat(accessToken: String, id: Int, pagination: BackwardPagination? = null): Chat {
    val data = operateReadChat(accessToken, id, pagination).data!!["readChat"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errReadChat(accessToken: String, id: Int, pagination: BackwardPagination? = null): String =
    operateReadChat(accessToken, id, pagination).errors!![0].message

class ReadChatTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("The chat should be read") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        readChat(token, chatId).id shouldBe chatId
    }

    test("Requesting a chat using an invalid ID should return an error") {
        val token = createSignedInUsers(1)[0].accessToken
        errReadChat(id = 1, accessToken = token) shouldBe InvalidChatIdException.message
    }

    test("The private chat the user just deleted should be readThe chat should be read") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        readChat(user1.accessToken, chatId)
    }

    test("Messages should be paginated") { testPagination(OperationName.READ_CHAT) }
}