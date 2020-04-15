package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun searchChats(query: String, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("query", query) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "chats-search?$parameters") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

class GetChatsSearchTest : StringSpec({
    listener(AppListener())

    fun createPrivateChats(jwt: String): List<Int> = listOf(
        NewAccount(username = "Iron Man", password = "malibu", email = "tony@stark.com", firstName = "Tony"),
        NewAccount(username = "Iron Fist", password = "monk", email = "iron.fist@monks.org"),
        NewAccount(username = "Chris Tony", password = "pass", email = "chris@example.com", lastName = "Tony")
    ).map {
        createAccount(it)
        val userId = Auth.findUserByUsername(it.username).id
        gson.fromJson(createPrivateChat(userId, jwt).content, ChatId::class.java).id
    }

    fun createGroupChats(jwt: String): List<Int> {
        val userIdList = setOf(createVerifiedUsers(1)[0].id)
        return listOf(
            GroupChat(userIdList, "Iron Man Fan Club"),
            GroupChat(userIdList, "Language Class"),
            GroupChat(userIdList, "Programming Languages"),
            GroupChat(userIdList, "Tony's Birthday")
        ).map { gson.fromJson(createGroupChat(it, jwt).content, ChatId::class.java).id }
    }

    fun testSearch(query: String, results: List<Chat>, jwt: String) {
        val response = searchChats(query, jwt)
        response.status() shouldBe HttpStatusCode.OK
        withClue("query: $query") { gson.fromJson(response.content, Chats::class.java) shouldBe Chats(results) }
    }

    "Private chats and group chats should be searched case-insensitively" {
        val jwt = getJwt(createVerifiedUsers(1)[0].login)
        val privateChatIdList = createPrivateChats(jwt)
        val groupChatIdList = createGroupChats(jwt)
        val buildPrivateChat = { chatIdIndex: Int -> Chat(ChatType.PRIVATE, privateChatIdList[chatIdIndex]) }
        val buildGroupChat = { chatIdIndex: Int -> Chat(ChatType.GROUP, groupChatIdList[chatIdIndex]) }
        testSearch("iron", listOf(buildPrivateChat(0), buildPrivateChat(1), buildGroupChat(0)), jwt)
        testSearch("tony", listOf(buildPrivateChat(0), buildPrivateChat(2), buildGroupChat(3)), jwt)
        testSearch("language", listOf(buildGroupChat(1), buildGroupChat(2)), jwt)
        testSearch("an f", listOf(buildGroupChat(0)), jwt)
        testSearch("Harry Potter", listOf(), jwt)
    }

    "A query which happens to match the user shouldn't return all the results" {
        val accounts = listOf(
            NewAccount(username = "john_doe", password = "pass", email = "john.doe@example.com", firstName = "John"),
            NewAccount("username", "password", "username@example.com")
        )
        for (account in accounts) createAccount(account)
        val response = with(accounts[0]) {
            Auth.verifyEmail(username)
            searchChats("John", getJwt(Login(username, password)))
        }
        gson.fromJson(response.content, Chats::class.java).chats.shouldBeEmpty()
    }
})