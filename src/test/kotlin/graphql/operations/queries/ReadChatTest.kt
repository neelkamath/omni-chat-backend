package graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import graphql.operations.GROUP_CHAT_FRAGMENT
import graphql.operations.PRIVATE_CHAT_FRAGMENT
import graphql.operations.mutations.createGroupChat
import graphql.operations.mutations.createPrivateChat
import graphql.operations.mutations.deletePrivateChat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val READ_CHAT_QUERY = """
    query ReadChat(
        ${"$"}id: Int!
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
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
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    READ_CHAT_QUERY,
    variables = mapOf(
        "id" to id,
        "groupChat_messages_last" to messagesPagination?.last,
        "groupChat_messages_before" to messagesPagination?.before?.toString(),
        "privateChat_messages_last" to messagesPagination?.last,
        "privateChat_messages_before" to messagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString()
    ),
    accessToken = accessToken
)

fun readChat(
    accessToken: String,
    id: Int,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): Chat {
    val data = operateReadChat(accessToken, id, usersPagination, messagesPagination).data!!["readChat"] as Map<*, *>
    return objectMapper.convertValue(data)
}

fun errReadChat(
    accessToken: String,
    id: Int,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): String = operateReadChat(accessToken, id, usersPagination, messagesPagination).errors!![0].message

class ReadChatTest : FunSpec({
    test("The chat should be read") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
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