package com.neelkamath.omniChat.test.graphql.api.queries

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.GROUP_CHAT_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.PRIVATE_CHAT_FRAGMENT
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.verifyEmailAddress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val SEARCH_CHATS_QUERY: String = """
    query SearchChats(${"$"}query: String!) {
        searchChats(query: ${"$"}query) {
            __typename
            $GROUP_CHAT_FRAGMENT
            $PRIVATE_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchChats(query: String, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(SEARCH_CHATS_QUERY, variables = mapOf("query" to query), accessToken = accessToken)

fun searchChats(query: String, accessToken: String): List<Chat> {
    val chats = operateSearchChats(query, accessToken).data!!["searchChats"] as List<*>
    return chats.map(::convertChat)
}

class SearchChatsTest : FunSpec({
    listener(AppListener())

    fun createPrivateChats(accessToken: String): List<PrivateChat> = listOf(
        NewAccount(username = "iron man", password = "malibu", emailAddress = "tony@stark.com", firstName = "Tony"),
        NewAccount(username = "iron fist", password = "monk", emailAddress = "iron.fist@monks.org"),
        NewAccount(username = "chris tony", password = "pass", emailAddress = "chris@example.com", lastName = "Tony")
    ).map {
        createAccount(it)
        val userId = findUserByUsername(it.username).id
        val chatId = createPrivateChat(userId, accessToken)
        PrivateChat(chatId, userId, Messages.read(chatId))
    }

    fun createGroupChats(adminId: String, accessToken: String): List<GroupChat> = listOf(
        NewGroupChat("Iron Man Fan Club"),
        NewGroupChat("Language Class"),
        NewGroupChat("Programming Languages"),
        NewGroupChat("Tony's Birthday")
    ).map {
        val chatId = createGroupChat(it, accessToken)
        GroupChat(chatId, adminId, it.userIdList + adminId, it.title, it.description, Messages.read(chatId))
    }

    test("Private chats and group chats should be searched case-insensitively") {
        val user = createVerifiedUsers(1)[0]
        val privateChats = createPrivateChats(user.accessToken)
        val groupChats = createGroupChats(user.info.id, user.accessToken)
        searchChats("iron", user.accessToken) shouldBe listOf(privateChats[0], privateChats[1], groupChats[0])
        searchChats("tony", user.accessToken) shouldBe listOf(privateChats[0], privateChats[2], groupChats[3])
        searchChats("language", user.accessToken) shouldBe listOf(groupChats[1], groupChats[2])
        searchChats("an f", user.accessToken) shouldBe listOf(groupChats[0])
        searchChats("Harry Potter", user.accessToken).shouldBeEmpty()
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
            searchChats("John", token)
        }
        response.shouldBeEmpty()
    }
})