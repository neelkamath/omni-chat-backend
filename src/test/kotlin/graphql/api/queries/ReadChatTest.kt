package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.test.graphql.api.Cursor
import com.neelkamath.omniChat.test.graphql.api.buildGroupChatFragment
import com.neelkamath.omniChat.test.graphql.api.buildPrivateChatFragment
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

fun buildReadChatQuery(last: Int?, before: Cursor?): String = """
    query ReadChat(${"$"}id: Int!) {
        readChat(id: ${"$"}id) {
            ${buildPrivateChatFragment(last, before)}
            ${buildGroupChatFragment(last, before)}
        }
    }
"""

private fun operateReadChat(accessToken: String, id: Int, last: Int? = null, before: Cursor? = null): GraphQlResponse =
    operateQueryOrMutation(buildReadChatQuery(last, before), variables = mapOf("id" to id), accessToken = accessToken)

fun readChat(accessToken: String, id: Int, last: Int? = null, before: Cursor? = null): Chat {
    val data = operateReadChat(accessToken, id, last, before).data!!["readChat"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errReadChat(accessToken: String, id: Int, last: Int? = null, before: Cursor? = null): String =
    operateReadChat(accessToken, id, last, before).errors!![0].message

class ReadChatTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("The chat should be read") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        val chat = readChat(token, chatId) as GroupChat
        chat.id shouldBe chatId
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
}