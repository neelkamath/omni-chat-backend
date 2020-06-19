package com.neelkamath.omniChat.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.api.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.api.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.graphql.api.mutations.deletePrivateChat
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val SEARCH_CHATS_QUERY: String = """
    query SearchChats(${"$"}query: String!, ${"$"}last: Int, ${"$"}before: Cursor) {
        searchChats(query: ${"$"}query) {
            $GROUP_CHAT_FRAGMENT
            $PRIVATE_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchChats(
    accessToken: String,
    query: String,
    pagination: BackwardPagination? = null
): GraphQlResponse = operateQueryOrMutation(
    SEARCH_CHATS_QUERY,
    variables = mapOf("query" to query, "last" to pagination?.last, "before" to pagination?.before?.toString()),
    accessToken = accessToken
)

fun searchChats(accessToken: String, query: String, pagination: BackwardPagination? = null): List<Chat> {
    val chats = operateSearchChats(accessToken, query, pagination).data!!["searchChats"] as List<*>
    return objectMapper.convertValue(chats)
}

class SearchChatsTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    fun createPrivateChats(accessToken: String): List<PrivateChat> = listOf(
        NewAccount(username = "iron man", password = "malibu", emailAddress = "tony@example.com", firstName = "Tony"),
        NewAccount(username = "iron fist", password = "monk", emailAddress = "iron.fist@example.org"),
        NewAccount(username = "chris tony", password = "pass", emailAddress = "chris@example.com", lastName = "Tony")
    ).map {
        createAccount(it)
        val userId = findUserByUsername(it.username).id
        val chatId = createPrivateChat(accessToken, userId)
        PrivateChat(chatId, findUserById(userId), Messages.readPrivateChatConnection(chatId, userId))
    }

    fun createGroupChats(accessToken: String, adminId: String): List<GroupChat> = listOf(
        NewGroupChat("Iron Man Fan Club"),
        NewGroupChat("Language Class"),
        NewGroupChat("Programming Languages"),
        NewGroupChat("Tony's Birthday")
    ).map {
        val chatId = createGroupChat(accessToken, it)
        val users = (it.userIdList + adminId).map(::findUserById)
        GroupChat(chatId, adminId, users, it.title, it.description, Messages.readGroupChatConnection(chatId))
    }

    test("Private chats and group chats should be searched case-insensitively") {
        val user = createSignedInUsers(1)[0]
        val privateChats = createPrivateChats(user.accessToken)
        val groupChats = createGroupChats(user.accessToken, user.info.id)
        searchChats(user.accessToken, "iron") shouldBe listOf(privateChats[0], privateChats[1], groupChats[0])
        searchChats(user.accessToken, "tony") shouldBe listOf(privateChats[0], privateChats[2], groupChats[3])
        searchChats(user.accessToken, "language") shouldBe listOf(groupChats[1], groupChats[2])
        searchChats(user.accessToken, "an f") shouldBe listOf(groupChats[0])
        searchChats(user.accessToken, "Harry Potter").shouldBeEmpty()
    }

    test("A query which matches the user shouldn't return every chat they're in") {
        val accounts = listOf(
            NewAccount(
                username = "john_doe",
                password = "pass",
                emailAddress = "john.doe@example.com",
                firstName = "John"
            ),
            NewAccount("username", "password", "username@example.com")
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
        searchChats(user1.accessToken, user2.info.username).shouldBeEmpty()
    }

    test("Messages should be paginated") { testPagination(OperationName.SEARCH_CHATS) }
}