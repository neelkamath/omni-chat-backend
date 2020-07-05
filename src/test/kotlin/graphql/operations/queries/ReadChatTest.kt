package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CHAT_QUERY = """
    query ReadChat(
        ${"$"}id: Int!
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChat(id: ${"$"}id) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChat(
    accessToken: String,
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    READ_CHAT_QUERY,
    variables = mapOf(
        "id" to id,
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    accessToken = accessToken
)

fun readChat(
    accessToken: String,
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): Chat {
    val data = operateReadChat(
        accessToken,
        id,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["readChat"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errReadChat(
    accessToken: String,
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): String = operateReadChat(
    accessToken,
    id,
    privateChatMessagesPagination,
    usersPagination,
    groupChatMessagesPagination
).errors!![0].message

class ReadChatTest : FunSpec({
    test("The chat should be read") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, buildNewGroupChat())
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

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.READ_CHAT) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHAT)
    }
})