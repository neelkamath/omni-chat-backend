package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.ChatMessage
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.jsonMapper
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.CHAT_MESSAGE_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.mutations.readCreatedMessageId
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val SEARCH_MESSAGES_QUERY: String = """
    query SearchMessages(${"$"}query: String!) {
        searchMessages(query: ${"$"}query) {
            $CHAT_MESSAGE_FRAGMENT
        }
    }
"""

private fun operateSearchMessages(query: String, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(SEARCH_MESSAGES_QUERY, variables = mapOf("query" to query), accessToken = accessToken)

fun searchMessages(query: String, accessToken: String): List<ChatMessage> {
    val data = operateSearchMessages(query, accessToken).data!!["searchMessages"] as List<*>
    return jsonMapper.convertValue(data)
}

class SearchMessagesTest : FunSpec({
    listener(AppListener())

    test("Messages should be search case-insensitively only across chats the user is in") {
        val (user1, user2, user3) = createVerifiedUsers(3)
        val chat1Id = createPrivateChat(user2.info.id, user1.accessToken)
        val message1Id = readCreatedMessageId(chat1Id, "Hey!", user1.accessToken)
        val chat2Id = createPrivateChat(user3.info.id, user1.accessToken)
        createMessage(chat2Id, "hiii", user1.accessToken)
        val message2Id = readCreatedMessageId(chat2Id, "hey, what's up?", user3.accessToken)
        createMessage(chat2Id, "sitting, wbu?", user1.accessToken)
        val chat3Id = createPrivateChat(user3.info.id, user2.accessToken)
        createMessage(chat3Id, "hey", user2.accessToken)
        val messageIdList = searchMessages("hey", user1.accessToken).flatMap { it.messages }.map { it.id }
        messageIdList shouldBe listOf(message1Id, message2Id)
    }
})