package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.jsonMapper
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.MESSAGE_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SEARCH_CHAT_MESSAGES_QUERY: String = """
    query SearchChatMessages(${"$"}chatId: Int!, ${"$"}query: String!) {
        searchChatMessages(chatId: ${"$"}chatId, query: ${"$"}query) {
            $MESSAGE_FRAGMENT
        }
    }
"""

private fun operateSearchChatMessages(chatId: Int, query: String, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(
        SEARCH_CHAT_MESSAGES_QUERY,
        variables = mapOf("chatId" to chatId, "query" to query),
        accessToken = accessToken
    )

fun searchChatMessages(chatId: Int, query: String, accessToken: String): List<Message> {
    val data = operateSearchChatMessages(chatId, query, accessToken).data!!["searchChatMessages"] as List<*>
    return jsonMapper.convertValue(data)
}

fun errSearchChatMessages(chatId: Int, query: String, accessToken: String): String =
    operateSearchChatMessages(chatId, query, accessToken).errors!![0].message

class SearchChatMessagesTest : FunSpec({
    listener(AppListener())

    test("Messages should be searched case-insensitively") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        createMessage(chatId, "Hey", user1.accessToken)
        createMessage(chatId, "hey", user2.accessToken)
        createMessage(chatId, "How are you?", user1.accessToken)
        searchChatMessages(chatId, "hey", user1.accessToken) shouldBe Messages.readChat(chatId).dropLast(1)
    }

    test("Searching in a chat the user isn't in should return an error") {
        val (user1, user2, user3) = createVerifiedUsers(3)
        val chatId = createPrivateChat(user3.info.id, user2.accessToken)
        errSearchChatMessages(chatId, "query", user1.accessToken) shouldBe InvalidChatIdException().message
    }
})