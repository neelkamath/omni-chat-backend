package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.ChatMessages
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.TextMessage
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CHAT_MESSAGES_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.messageAndReadId
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val SEARCH_MESSAGES_QUERY = """
    query SearchMessages(
        ${"$"}query: String!
        ${"$"}chatMessages_messages_last: Int
        ${"$"}chatMessages_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchMessages(query: ${"$"}query) {
            $CHAT_MESSAGES_FRAGMENT
        }
    }
"""

private fun operateSearchMessages(
    accessToken: String,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    SEARCH_MESSAGES_QUERY,
    variables = mapOf(
        "query" to query,
        "chatMessages_messages_last" to chatMessagesPagination?.last,
        "chatMessages_messages_before" to chatMessagesPagination?.before?.toString(),
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    accessToken = accessToken
)

fun searchMessages(
    accessToken: String,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<ChatMessages> {
    val messages = operateSearchMessages(
        accessToken,
        query,
        chatMessagesPagination,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["searchMessages"] as List<*>
    return objectMapper.convertValue(messages)
}

class SearchMessagesTest : FunSpec({
    test("Messages should be search case-insensitively only across chats the user is in") {
        val (user1, user2, user3) = createSignedInUsers(3)
        val chat1Id = createPrivateChat(user1.accessToken, user2.info.id)
        val message1Id = messageAndReadId(user1.accessToken, chat1Id, TextMessage("Hey!"))
        val chat2Id = createPrivateChat(user1.accessToken, user3.info.id)
        createMessage(user1.accessToken, chat2Id, TextMessage("hiii"))
        val message2Id = messageAndReadId(user3.accessToken, chat2Id, TextMessage("hey, what's up?"))
        createMessage(user1.accessToken, chat2Id, TextMessage("sitting, wbu?"))
        val chat3Id = createPrivateChat(user2.accessToken, user3.info.id)
        createMessage(user2.accessToken, chat3Id, TextMessage("hey"))
        searchMessages(user1.accessToken, "hey").flatMap { it.messages }.map { it.cursor } shouldBe
                listOf(message1Id, message2Id)
    }

    test("Messages from a private chat the user deleted shouldn't be included in the search results") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val text = "text"
        messageAndReadId(user1.accessToken, chatId, TextMessage(text))
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
        messageAndReadId(user1.accessToken, chatId, TextMessage(text))
        deletePrivateChat(user1.accessToken, chatId)
        searchMessages(user1.accessToken, text).shouldBeEmpty()
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_MESSAGES) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)
    }
})