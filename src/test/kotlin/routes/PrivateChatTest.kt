package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.PrivateChat
import com.neelkamath.omniChat.db.PrivateChatClears
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.test.db.read
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun createPrivateChat(userId: String, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("user_id", userId) }.formUrlEncode()
    handleRequest(HttpMethod.Post, "private-chat?$parameters") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun deletePrivateChat(chatId: Int, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("chat_id", chatId.toString()) }.formUrlEncode()
    handleRequest(HttpMethod.Delete, "private-chat?$parameters") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

class DeletePrivateChatTest : StringSpec({
    listener(AppListener())

    "A chat should be deleted" {
        val users = createVerifiedUsers(2)
        val jwt = getJwt(users[0].login)
        val response = createPrivateChat(users[1].id, jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        deletePrivateChat(chatId, jwt).status() shouldBe HttpStatusCode.NoContent
        withClue("The list of chat deletions should be from the creator of the chat") {
            PrivateChatClears.read(chatId) shouldBe listOf(true)
        }
    }

    "Deleting an invalid chat ID should respond with an HTTP status code of 400" {
        val (login) = createVerifiedUsers(1)[0]
        deletePrivateChat(chatId = 1, jwt = getJwt(login)).status() shouldBe HttpStatusCode.BadRequest
    }
})

class PostPrivateChatTest : StringSpec({
    listener(AppListener())

    "A chat should be created" {
        val users = createVerifiedUsers(2)
        val creator = users[0]
        val invitedUser = users[1].id
        val response = createPrivateChat(invitedUser, getJwt(creator.login))
        response.status() shouldBe HttpStatusCode.OK
        val body = gson.fromJson(response.content, ChatId::class.java)
        PrivateChats.read(creator.id) shouldBe listOf(PrivateChat(body.id, creator.id, invitedUser))
    }

    fun testBadResponse(response: TestApplicationResponse, reason: InvalidPrivateChatReason) {
        response.status() shouldBe HttpStatusCode.BadRequest
        gson.fromJson(response.content, InvalidPrivateChat::class.java) shouldBe InvalidPrivateChat(reason)
    }

    "An existing chat shouldn't be recreated" {
        val users = createVerifiedUsers(2)
        val inviteeId = users[1].id
        val jwt = getJwt(users[0].login)
        createPrivateChat(inviteeId, jwt)
        testBadResponse(createPrivateChat(inviteeId, jwt), InvalidPrivateChatReason.CHAT_EXISTS)
    }

    "A chat shouldn't be created with a nonexistent user" {
        val (login) = createVerifiedUsers(1)[0]
        val response = createPrivateChat("a nonexistent user ID", getJwt(login))
        testBadResponse(response, InvalidPrivateChatReason.INVALID_USER_ID)
    }

    "A chat shouldn't be created with the user themselves" {
        val (login, userId) = createVerifiedUsers(1)[0]
        testBadResponse(createPrivateChat(userId, getJwt(login)), InvalidPrivateChatReason.INVALID_USER_ID)
    }
})