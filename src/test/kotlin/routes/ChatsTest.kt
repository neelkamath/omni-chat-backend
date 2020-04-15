package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun readChats(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Get, "chats") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

class GetChatsTest : StringSpec({
    listener(AppListener())

    "Chats should be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = getJwt(users[0].login)
        val groupChatResponse = createGroupChat(GroupChat(setOf(users[1].id), "Title"), jwt)
        val groupChatId = gson.fromJson(groupChatResponse.content, ChatId::class.java).id
        val privateChatResponse = createPrivateChat(users[1].id, jwt)
        val privateChatId = gson.fromJson(privateChatResponse.content, ChatId::class.java).id
        val body = gson.fromJson(readChats(jwt).content, Chats::class.java)
        val groupChat = Chat(ChatType.GROUP, groupChatId)
        val privateChat = Chat(ChatType.PRIVATE, privateChatId)
        body shouldBe Chats(listOf(groupChat, privateChat))
    }

    "Chats deleted by the user shouldn't be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = getJwt(users[0].login)
        val response = createPrivateChat(users[1].id, jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        deletePrivateChat(chatId, jwt)
        gson.fromJson(readChats(jwt).content, Chats::class.java) shouldBe Chats(listOf())
    }

    "Chats deleted by the invitee, but not by the creator, should be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = getJwt(users[0].login)
        val response = createPrivateChat(users[1].id, jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        deletePrivateChat(chatId, getJwt(users[1].login))
        gson.fromJson(readChats(jwt).content, Chats::class.java) shouldBe Chats(listOf(Chat(ChatType.PRIVATE, chatId)))
    }
})