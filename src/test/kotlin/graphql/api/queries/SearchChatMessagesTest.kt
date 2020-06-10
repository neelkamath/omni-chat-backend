package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.MessageEdge
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.objectMapper
import com.neelkamath.omniChat.test.graphql.api.buildMessageEdgeFragment
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

fun buildSearchChatMessagesQuery(): String = """
    query SearchChatMessages(${"$"}chatId: Int!, ${"$"}query: String!) {
        searchChatMessages(chatId: ${"$"}chatId, query: ${"$"}query) {
            ${buildMessageEdgeFragment()}
        }
    }
"""

private fun operateSearchChatMessages(accessToken: String, chatId: Int, query: String): GraphQlResponse =
    operateQueryOrMutation(
        buildSearchChatMessagesQuery(),
        variables = mapOf("chatId" to chatId, "query" to query),
        accessToken = accessToken
    )

fun searchChatMessages(accessToken: String, chatId: Int, query: String): List<MessageEdge> {
    val data = operateSearchChatMessages(accessToken, chatId, query).data!!["searchChatMessages"] as List<*>
    return objectMapper.convertValue(data)
}

fun errSearchChatMessages(accessToken: String, chatId: Int, query: String): String =
    operateSearchChatMessages(accessToken, chatId, query).errors!![0].message

class SearchChatMessagesTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("Messages should be searched case-insensitively") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        createMessage(user1.accessToken, chatId, "Hey!")
        createMessage(user2.accessToken, chatId, ":) hey")
        createMessage(user1.accessToken, chatId, "How are you?")
        searchChatMessages(user1.accessToken, chatId, "hey") shouldBe
                Messages.readPrivateChat(chatId, user1.info.id).dropLast(1)
    }

    test(
        """
        Given a private chat in which the first user sent a message and then deleted the chat,
        when the users search for the message,
        then the first user shouldn't find it but the second user should
        """
    ) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val text = "text"
        createMessage(user1.accessToken, chatId, text)
        deletePrivateChat(user1.accessToken, chatId)
        searchChatMessages(user1.accessToken, chatId, text).shouldBeEmpty()
        searchChatMessages(user2.accessToken, chatId, text).shouldNotBeEmpty()
    }

    test("Searching in a chat the user isn't in should return an error") {
        val (user1, user2, user3) = createSignedInUsers(3)
        val chatId = createPrivateChat(user2.accessToken, user3.info.id)
        errSearchChatMessages(user1.accessToken, chatId, "query") shouldBe InvalidChatIdException.message
    }
}