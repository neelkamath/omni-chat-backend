package com.neelkamath.omniChat.graphql.operations.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.GroupChatUsers
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createAccount
import com.neelkamath.omniChat.graphql.operations.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.operations.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.operations.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

const val SEARCH_CHATS_QUERY = """
    query SearchChats(
        ${"$"}query: String!
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor 
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor 
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
    ) {
        searchChats(query: ${"$"}query) {
            $GROUP_CHAT_FRAGMENT
            $PRIVATE_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchChats(
    accessToken: String,
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): GraphQlResponse = operateGraphQlQueryOrMutation(
    SEARCH_CHATS_QUERY,
    variables = mapOf(
        "query" to query,
        "groupChat_messages_last" to messagesPagination?.last,
        "groupChat_messages_before" to messagesPagination?.before?.toString(),
        "privateChat_messages_last" to messagesPagination?.last,
        "privateChat_messages_before" to messagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString()
    ),
    accessToken = accessToken
)

fun searchChats(
    accessToken: String,
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateSearchChats(accessToken, query, usersPagination, messagesPagination)
        .data!!["searchChats"] as List<*>
    return objectMapper.convertValue(chats)
}

class SearchChatsTest : FunSpec({
    fun createPrivateChats(accessToken: String): List<PrivateChat> = listOf(
        NewAccount(Username("iron man"), Password("malibu"), "tony@example.com", firstName = "Tony"),
        NewAccount(Username("iron fist"), Password("monk"), "iron.fist@example.org"),
        NewAccount(Username("chris tony"), Password("pass"), "chris@example.com", lastName = "Tony")
    ).map {
        createAccount(it)
        val userId = readUserByUsername(it.username).id
        val chatId = createPrivateChat(accessToken, userId)
        PrivateChat(chatId, readUserById(userId), Messages.readPrivateChatConnection(chatId, userId))
    }

    fun createGroupChats(accessToken: String, adminId: String): List<GroupChat> = listOf(
        NewGroupChat(GroupChatTitle("Iron Man Fan Club"), GroupChatDescription("")),
        NewGroupChat(GroupChatTitle("Language Class"), GroupChatDescription("")),
        NewGroupChat(GroupChatTitle("Programming Languages"), GroupChatDescription("")),
        NewGroupChat(GroupChatTitle("Tony's Birthday"), GroupChatDescription(""))
    ).map {
        val chatId = createGroupChat(accessToken, it)
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
        val user = createSignedInUsers(1)[0]
        val privateChats = createPrivateChats(user.accessToken)
        val groupChats = createGroupChats(user.accessToken, user.info.id)
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
        accounts.forEach { createAccount(it) }
        val response = with(accounts[0]) {
            verifyEmailAddress(username)
            val token = requestTokenSet(Login(username, password)).accessToken
            searchChats(token, "John")
        }
        response.shouldBeEmpty()
    }

    test("Searching a private chat the user deleted shouldn't include the chat in the search results") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        searchChats(user1.accessToken, user2.info.username.value).shouldBeEmpty()
    }

    test("Messages should be paginated") { testMessagesPagination(MessagesOperationName.SEARCH_CHATS) }

    test("Group chat users should be paginated") {
        testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_CHATS)
    }
})