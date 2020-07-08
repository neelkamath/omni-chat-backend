package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.PrivateChatDeletions
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.db.tables.message
import com.neelkamath.omniChat.graphql.operations.CHAT_MESSAGES_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
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
        val (user1, user2, user3) = createVerifiedUsers(3)
        val chat1Id = PrivateChats.create(user1.info.id, user2.info.id)
        val message1Id = Messages.message(chat1Id, user1.info.id, TextMessage("Hey!"))
        val chat2Id = PrivateChats.create(user1.info.id, user3.info.id)
        Messages.create(chat2Id, user1.info.id, TextMessage("hiii"))
        val message2Id = Messages.message(chat2Id, user3.info.id, TextMessage("hey, what's up?"))
        Messages.create(chat2Id, user1.info.id, TextMessage("sitting, wbu?"))
        val chat3Id = PrivateChats.create(user2.info.id, user3.info.id)
        Messages.create(chat3Id, user2.info.id, TextMessage("hey"))
        searchMessages(user1.accessToken, "hey").flatMap { it.messages }.map { it.cursor } shouldBe
                listOf(message1Id, message2Id)
    }

    test("Messages from a private chat the user deleted shouldn't be included in the search results") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val text = "text"
        Messages.message(chatId, user1.info.id, TextMessage(text))
        PrivateChatDeletions.create(chatId, user1.info.id)
        searchMessages(user1.accessToken, text).shouldBeEmpty()
    }

    test(
        """
        Given a user who created a private chat, sent a message, and deleted the chat,
        when searching for the message,
        then it shouldn't be retrieved
        """
    ) {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val text = "text"
        Messages.message(chatId, user1.info.id, TextMessage(text))
        PrivateChatDeletions.create(chatId, user1.info.id)
        searchMessages(user1.accessToken, text).shouldBeEmpty()
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_MESSAGES) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)
    }
})