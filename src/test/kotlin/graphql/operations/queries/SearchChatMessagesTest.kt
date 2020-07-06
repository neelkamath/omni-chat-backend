package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.MessageEdge
import com.neelkamath.omniChat.TextMessage
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.MESSAGE_EDGE_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

const val SEARCH_CHAT_MESSAGES_FRAGMENT = """
    query SearchChatMessages(${"$"}chatId: Int!, ${"$"}query: String!, ${"$"}last: Int, ${"$"}before: Cursor) {
        searchChatMessages(chatId: ${"$"}chatId, query: ${"$"}query, last: ${"$"}last, before: ${"$"}before) {
            $MESSAGE_EDGE_FRAGMENT
        }
    }
"""

private fun operateSearchChatMessages(
    accessToken: String,
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    SEARCH_CHAT_MESSAGES_FRAGMENT,
    variables = mapOf(
        "chatId" to chatId,
        "query" to query,
        "last" to pagination?.last,
        "before" to pagination?.before?.toString()
    ),
    accessToken = accessToken
)

fun searchChatMessages(
    accessToken: String,
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null
): List<MessageEdge> {
    val data =
        operateSearchChatMessages(accessToken, chatId, query, pagination).data!!["searchChatMessages"] as List<*>
    return objectMapper.convertValue(data)
}

fun errSearchChatMessages(
    accessToken: String,
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null
): String = operateSearchChatMessages(accessToken, chatId, query, pagination).errors!![0].message

class SearchChatMessagesTest : FunSpec({
    test("Messages should be searched case-insensitively") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        createMessage(user1.accessToken, chatId, TextMessage("Hey!"))
        createMessage(user2.accessToken, chatId, TextMessage(":) hey"))
        createMessage(user1.accessToken, chatId, TextMessage("How are you?"))
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
        createMessage(user1.accessToken, chatId, TextMessage(text))
        deletePrivateChat(user1.accessToken, chatId)
        searchChatMessages(user1.accessToken, chatId, text).shouldBeEmpty()
        searchChatMessages(user2.accessToken, chatId, text).shouldNotBeEmpty()
    }

    test("Searching in a chat the user isn't in should return an error") {
        val (user1, user2, user3) = createSignedInUsers(3)
        val chatId = createPrivateChat(user2.accessToken, user3.info.id)
        errSearchChatMessages(user1.accessToken, chatId, "query") shouldBe InvalidChatIdException.message
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_CHAT_MESSAGES) }
})