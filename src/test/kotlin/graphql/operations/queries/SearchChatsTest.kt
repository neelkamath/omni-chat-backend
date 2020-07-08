package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.operations.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

const val SEARCH_CHATS_QUERY = """
    query SearchChats(
        ${"$"}query: String!
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchChats(query: ${"$"}query) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchChats(
    accessToken: String,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    SEARCH_CHATS_QUERY,
    variables = mapOf(
        "query" to query,
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    accessToken = accessToken
)

fun searchChats(
    accessToken: String,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateSearchChats(
        accessToken,
        query,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["searchChats"] as List<*>
    return objectMapper.convertValue(chats)
}

class SearchChatsTest : FunSpec({
    fun createPrivateChats(userId: String): List<PrivateChat> = listOf(
        NewAccount(Username("iron man"), Password("malibu"), "tony@example.com", firstName = "Tony"),
        NewAccount(Username("iron fist"), Password("monk"), "iron.fist@example.org"),
        NewAccount(Username("chris tony"), Password("pass"), "chris@example.com", lastName = "Tony")
    ).map {
        createUser(it)
        val otherUserId = readUserByUsername(it.username).id
        val chatId = PrivateChats.create(userId, otherUserId)
        PrivateChat(chatId, readUserById(otherUserId), Messages.readPrivateChatConnection(chatId, otherUserId))
    }

    fun createGroupChats(adminId: String): List<GroupChat> = listOf(
        NewGroupChat(GroupChatTitle("Iron Man Fan Club"), GroupChatDescription("")),
        NewGroupChat(GroupChatTitle("Language Class"), GroupChatDescription("")),
        NewGroupChat(GroupChatTitle("Programming Languages"), GroupChatDescription("")),
        NewGroupChat(GroupChatTitle("Tony's Birthday"), GroupChatDescription(""))
    ).map {
        val chatId = GroupChats.create(adminId, it)
        GroupChat(
            chatId,
            adminId,
            GroupChatUsers.readUsers(chatId),
            it.title,
            it.description,
            Messages.readGroupChatConnection(chatId)
        )
    }

    test("Private chats and group chats should be searched case-insensitively") {
        val user = createVerifiedUsers(1)[0]
        val privateChats = createPrivateChats(user.info.id)
        val groupChats = createGroupChats(user.info.id)
        searchChats(user.accessToken, "iron") shouldContainExactlyInAnyOrder
                listOf(privateChats[0], privateChats[1], groupChats[0])
        searchChats(user.accessToken, "tony") shouldContainExactlyInAnyOrder
                listOf(privateChats[0], privateChats[2], groupChats[3])
        searchChats(user.accessToken, "language") shouldContainExactlyInAnyOrder listOf(groupChats[1], groupChats[2])
        searchChats(user.accessToken, "an f") shouldContainExactlyInAnyOrder listOf(groupChats[0])
        searchChats(user.accessToken, "Harry Potter").shouldBeEmpty()
    }

    test("A query which matches the user shouldn't return every chat they're in") {
        val accounts = listOf(
            NewAccount(
                Username("john_doe"),
                Password("pass"),
                emailAddress = "john.doe@example.com",
                firstName = "John"
            ),
            NewAccount(Username("username"), Password("password"), "username@example.com")
        )
        accounts.forEach(::createUser)
        val response = with(accounts[0]) {
            verifyEmailAddress(username)
            val userId = readUserByUsername(username).id
            val token = buildAuthToken(userId).accessToken
            searchChats(token, "John")
        }
        response.shouldBeEmpty()
    }

    test("Searching a private chat the user deleted shouldn't include the chat in the search results") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        PrivateChatDeletions.create(chatId, user1.info.id)
        searchChats(user1.accessToken, user2.info.username.value).shouldBeEmpty()
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_CHATS) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_CHATS)
    }
})