package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.ChatId
import com.neelkamath.omniChat.db.PrivateChat
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.gson
import com.neelkamath.omniChat.main
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
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

    "A chat shouldn't be created with a nonexistent user" {
        val (login) = createVerifiedUsers(1)[0]
        createPrivateChat("a nonexistent user ID", getJwt(login)).status() shouldBe HttpStatusCode.BadRequest
    }

    "A chat shouldn't be created with the user themselves" {
        val (login, userId) = createVerifiedUsers(1)[0]
        createPrivateChat(userId, getJwt(login)).status() shouldBe HttpStatusCode.BadRequest
        PrivateChats.read(userId).shouldBeEmpty()
    }
})