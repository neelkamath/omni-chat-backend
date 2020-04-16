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

    fun getChatId(response: TestApplicationResponse): Int = gson.fromJson(response.content, ChatId::class.java).id

    fun createChats(admin: CreatedUser, user: CreatedUser): List<Chat> {
        val adminJwt = getJwt(admin.login)
        val adminGroupChatResponse = createGroupChat(NewGroupChat(setOf(user.id), "Title"), adminJwt)
        val userGroupChatResponse = createGroupChat(NewGroupChat(setOf(admin.id), "Title"), getJwt(user.login))
        val createdPrivateChatResponse = createPrivateChat(user.id, adminJwt)
        val invitedPrivateChatResponse = createPrivateChat(admin.id, getJwt(createVerifiedUsers(1)[0].login))
        return listOf(
            Chat(ChatType.GROUP, getChatId(adminGroupChatResponse)),
            Chat(ChatType.GROUP, getChatId(userGroupChatResponse)),
            Chat(ChatType.PRIVATE, getChatId(createdPrivateChatResponse)),
            Chat(ChatType.PRIVATE, getChatId(invitedPrivateChatResponse))
        )
    }

    "Private and group chats the user made, was invited to, and was added to should be retrieved" {
        val (admin, user) = createVerifiedUsers(2)
        val chats = Chats(createChats(admin, user))
        val response = readChats(getJwt(admin.login))
        gson.fromJson(response.content, Chats::class.java) shouldBe chats
    }

    "Private chats deleted by the user shouldn't be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = getJwt(users[0].login)
        val response = createPrivateChat(users[1].id, jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        deletePrivateChat(chatId, jwt)
        gson.fromJson(readChats(jwt).content, Chats::class.java) shouldBe Chats(listOf())
    }

    "Group chats deleted by the user shouldn't be retrieved" {
        val (admin, user) = createVerifiedUsers(2)
        val jwt = getJwt(admin.login)
        val response = createGroupChat(NewGroupChat(setOf(user.id), "Title"), jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        leaveGroupChat(jwt, chatId, newAdminUserId = user.id)
        gson.fromJson(readChats(jwt).content, Chats::class.java) shouldBe Chats(listOf())
    }

    "Chats deleted only by the invitee should be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = getJwt(users[0].login)
        val response = createPrivateChat(users[1].id, jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        deletePrivateChat(chatId, getJwt(users[1].login))
        gson.fromJson(readChats(jwt).content, Chats::class.java) shouldBe Chats(listOf(Chat(ChatType.PRIVATE, chatId)))
    }
})