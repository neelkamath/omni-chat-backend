package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.ChatMessages
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.graphql.api.Cursor
import com.neelkamath.omniChat.test.graphql.api.buildChatMessagesFragment
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.test.graphql.api.mutations.messageAndReadId
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

fun buildSearchMessagesQuery(last: Int?, before: Cursor?): String = """
    query SearchMessages(${"$"}query: String!) {
        searchMessages(query: ${"$"}query) {
            ${buildChatMessagesFragment(last, before)}
        }
    }
"""

private fun operateSearchMessages(
    accessToken: String,
    query: String,
    last: Int? = null,
    before: Cursor? = null
): GraphQlResponse = operateQueryOrMutation(
    buildSearchMessagesQuery(last, before),
    variables = mapOf("query" to query),
    accessToken = accessToken
)

fun searchMessages(accessToken: String, query: String, last: Int? = null, before: Cursor? = null): List<ChatMessages> {
    val messages = operateSearchMessages(accessToken, query, last, before).data!!["searchMessages"] as List<*>
    return objectMapper.convertValue(messages)
}

class SearchMessagesTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Messages should be search case-insensitively only across chats the user is in") {
        val (user1, user2, user3) = createSignedInUsers(3)
        val chat1Id = createPrivateChat(user1.accessToken, user2.info.id)
        val message1Id = messageAndReadId(user1.accessToken, chat1Id, "Hey!")
        val chat2Id = createPrivateChat(user1.accessToken, user3.info.id)
        createMessage(user1.accessToken, chat2Id, "hiii")
        val message2Id = messageAndReadId(user3.accessToken, chat2Id, "hey, what's up?")
        createMessage(user1.accessToken, chat2Id, "sitting, wbu?")
        val chat3Id = createPrivateChat(user2.accessToken, user3.info.id)
        createMessage(user2.accessToken, chat3Id, "hey")
        val messageIdList = searchMessages(user1.accessToken, "hey").flatMap { it.messages }.map { it.node.id }
        messageIdList shouldBe listOf(message1Id, message2Id)
    }

    test("Messages from a private chat the user deleted shouldn't be included in the search results") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val text = "text"
        messageAndReadId(user1.accessToken, chatId, text)
        deletePrivateChat(user1.accessToken, chatId)
        searchMessages(user1.accessToken, text).shouldBeEmpty()
    }

    test(
        """
        Given a user who created a private chat, sent a message, and deleted the chat,
        when searching for the message,
        then it shouldn't be retrieved
        """
    ) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val text = "text"
        messageAndReadId(user1.accessToken, chatId, text)
        deletePrivateChat(user1.accessToken, chatId)
        searchMessages(user1.accessToken, text).shouldBeEmpty()
    }
}