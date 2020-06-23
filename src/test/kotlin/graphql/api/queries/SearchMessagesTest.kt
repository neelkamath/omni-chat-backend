package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.ChatMessages
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.graphql.api.CHAT_MESSAGES_FRAGMENT
import com.neelkamath.omniChat.graphql.api.messageAndReadId
import com.neelkamath.omniChat.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val SEARCH_MESSAGES_QUERY: String = """
    query SearchMessages(
        ${"$"}query: String!
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}chatMessages_messages_last: Int
        ${"$"}chatMessages_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
    ) {
        searchMessages(query: ${"$"}query) {
            $CHAT_MESSAGES_FRAGMENT
        }
    }
"""

private fun operateSearchMessages(
    accessToken: String,
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): GraphQlResponse = operateQueryOrMutation(
    SEARCH_MESSAGES_QUERY,
    variables = mapOf(
        "query" to query,
        "groupChat_messages_last" to messagesPagination?.last,
        "groupChat_messages_before" to messagesPagination?.before?.toString(),
        "privateChat_messages_last" to messagesPagination?.last,
        "privateChat_messages_before" to messagesPagination?.before?.toString(),
        "chatMessages_messages_last" to messagesPagination?.last,
        "chatMessages_messages_before" to messagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString()
    ),
    accessToken = accessToken
)

fun searchMessages(
    accessToken: String,
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): List<ChatMessages> {
    val messages = operateSearchMessages(accessToken, query, usersPagination, messagesPagination)
        .data!!["searchMessages"] as List<*>
    return objectMapper.convertValue(messages)
}

class SearchMessagesTest : FunSpec({
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
        searchMessages(user1.accessToken, "hey").flatMap { it.messages }.map { it.cursor } shouldBe
                listOf(message1Id, message2Id)
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

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_MESSAGES) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)
    }
})