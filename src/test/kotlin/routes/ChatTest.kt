package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.test.db.PrivateChat
import com.neelkamath.omniChat.test.db.read
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

fun createGroupChat(chat: GroupChat, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "group-chat") {
        addHeader(HttpHeaders.Authorization, "Bearer $jwt")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(gson.toJson(chat))
    }
}.response

fun createPrivateChat(userId: String, jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("user_id", userId) }.formUrlEncode()
    handleRequest(HttpMethod.Post, "private-chat?$parameters") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

class PostGroupChatTest : StringSpec({
    listener(AppListener())

    "A group chat should be created ignoring the user's own user ID" {
        val users = createVerifiedUsers(3)
        val chat = GroupChat(
            setOf(users[0].userId, users[1].userId, users[2].userId),
            title = "\uD83D\uDCDA Book Club",
            description = "Books discussion"
        )
        createGroupChat(chat, getJwt(users[0].login))
        GroupChats.read() shouldBe listOf(chat)
    }

    fun testInvalidChat(users: List<CreatedUser>, chat: GroupChat, reason: InvalidGroupChatReason) {
        val response = createGroupChat(chat, getJwt(users[0].login))
        response.status() shouldBe HttpStatusCode.BadRequest
        gson.fromJson(response.content, InvalidGroupChat::class.java).reason shouldBe reason
    }

    "A group chat should not be created when supplied with an invalid user ID" {
        val users = createVerifiedUsers(2)
        val chat = GroupChat(setOf(users[1].userId, "invalid user ID"), "Group Chat Title")
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_USER_ID)
    }

    "A group chat should not be created if there the user ID list is empty" {
        val users = createVerifiedUsers(1)
        val chat = GroupChat(userIdList = setOf(), title = "Group Chat")
        testInvalidChat(users, chat, InvalidGroupChatReason.EMPTY_USER_ID_LIST)
    }

    "A group chat should not be created if an empty title is supplied" {
        val users = createVerifiedUsers(2)
        val chat = GroupChat(setOf(users[1].userId), title = "")
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_TITLE_LENGTH)
    }

    "A group chat should not be created if the title is too long" {
        val users = createVerifiedUsers(2)
        val title = CharArray(GroupChats.maxTitleLength + 1) { 'a' }.joinToString("")
        val chat = GroupChat(setOf(users[1].userId), title)
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_TITLE_LENGTH)
    }

    "A group chat should not be created if the description has an invalid length" {
        val users = createVerifiedUsers(2)
        val description = CharArray(GroupChats.maxDescriptionLength + 1) { 'a' }.joinToString("")
        val chat = GroupChat(setOf(users[1].userId), "Group Chat Title", description)
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_DESCRIPTION_LENGTH)
    }
})

class PostPrivateChatTest : StringSpec({
    listener(AppListener())

    "A chat should be created" {
        val users = createVerifiedUsers(2)
        val creator = users[0]
        val invitedUser = users[1].userId
        createPrivateChat(invitedUser, getJwt(creator.login)).status() shouldBe HttpStatusCode.NoContent
        PrivateChats.read() shouldBe listOf(PrivateChat(creator.userId, invitedUser))
    }

    fun testInvalidChat(isValidUserId: Boolean) {
        val users = createVerifiedUsers(1)
        val userId = if (isValidUserId) users[0].userId else "a nonexistent user ID"
        createPrivateChat(userId, getJwt(users[0].login)).status() shouldBe HttpStatusCode.BadRequest
        PrivateChats.read().shouldBeEmpty()
    }

    "A chat shouldn't be created with a nonexistent user" { testInvalidChat(true) }

    "A chat shouldn't be created with the user themselves" { testInvalidChat(false) }
})