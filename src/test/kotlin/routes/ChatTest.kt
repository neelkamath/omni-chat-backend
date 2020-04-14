package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChatWithId
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.PrivateChat
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.test.db.readChat
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

fun readChats(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Get, "chats") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun updateGroupChat(update: GroupChatUpdate, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "group-chat") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(update))
        }
    }.response

class PostGroupChatTest : StringSpec({
    listener(AppListener())

    "A group chat should be created, ignoring the user's own user ID" {
        val users = createVerifiedUsers(3)
        val creator = users[0]
        val chat = GroupChat(
            setOf(creator.id, users[1].id, users[2].id),
            title = "\uD83D\uDCDA Book Club",
            description = "Books discussion"
        )
        val response = createGroupChat(chat, getJwt(creator.login))
        response.status() shouldBe HttpStatusCode.OK
        val body = gson.fromJson(response.content, ChatId::class.java)
        GroupChats.readCreated(creator.id) shouldBe listOf(GroupChatWithId(body.id, chat))
    }

    fun testInvalidChat(users: List<CreatedUser>, chat: GroupChat, reason: InvalidGroupChatReason) {
        val response = createGroupChat(chat, getJwt(users[0].login))
        response.status() shouldBe HttpStatusCode.BadRequest
        gson.fromJson(response.content, InvalidGroupChat::class.java).reason shouldBe reason
    }

    "A group chat should not be created when supplied with an invalid user ID" {
        val users = createVerifiedUsers(2)
        val chat = GroupChat(setOf(users[1].id, "invalid user ID"), "Group Chat Title")
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_USER_ID)
    }

    "A group chat should not be created if there the user ID list is empty" {
        val users = createVerifiedUsers(1)
        val chat = GroupChat(userIdList = setOf(), title = "Group Chat")
        testInvalidChat(users, chat, InvalidGroupChatReason.EMPTY_USER_ID_LIST)
    }

    "A group chat should not be created if an empty title is supplied" {
        val users = createVerifiedUsers(2)
        val chat = GroupChat(setOf(users[1].id), title = "")
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_TITLE_LENGTH)
    }

    "A group chat should not be created if the title is too long" {
        val users = createVerifiedUsers(2)
        val title = CharArray(GroupChats.maxTitleLength + 1) { 'a' }.joinToString("")
        val chat = GroupChat(setOf(users[1].id), title)
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_TITLE_LENGTH)
    }

    "A group chat should not be created if the description has an invalid length" {
        val users = createVerifiedUsers(2)
        val description = CharArray(GroupChats.maxDescriptionLength + 1) { 'a' }.joinToString("")
        val chat = GroupChat(setOf(users[1].id), "Group Chat Title", description)
        testInvalidChat(users, chat, InvalidGroupChatReason.INVALID_DESCRIPTION_LENGTH)
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
})

class PatchGroupChatTest : StringSpec({
    listener(AppListener())

    "A group chat should update" {
        val users = createVerifiedUsers(3)
        val jwt = getJwt(users[0].login)
        val initialUserIdList = setOf(users[1].id)
        val createdChatResponse = createGroupChat(GroupChat(initialUserIdList, "Title"), jwt)
        val chatId = gson.fromJson(createdChatResponse.content, ChatId::class.java).id
        val update = GroupChatUpdate(
            chatId,
            "New Title",
            "New Description",
            newUserIdList = setOf(users[2].id),
            removedUserIdList = setOf(users[1].id)
        )
        val response = updateGroupChat(update, jwt)
        response.status() shouldBe HttpStatusCode.NoContent
        val userIdList = initialUserIdList + update.newUserIdList!! - update.removedUserIdList!!
        GroupChats.readChat(chatId) shouldBe GroupChat(userIdList, update.title!!, update.description)
    }

    "Updating a nonexistent group chat should respond with an HTTP status code of 400" {
        val (login) = createVerifiedUsers(1)[0]
        updateGroupChat(GroupChatUpdate(chatId = 5), getJwt(login)).status() shouldBe HttpStatusCode.BadRequest
    }

    "Updating a group chat the user isn't the admin of should respond with an HTTP status code of 401" {
        val users = createVerifiedUsers(2)
        val createdChatResponse = createGroupChat(GroupChat(setOf(users[1].id), "Title"), getJwt(users[0].login))
        val chatId = gson.fromJson(createdChatResponse.content, ChatId::class.java).id
        updateGroupChat(GroupChatUpdate(chatId), getJwt(users[1].login)).status() shouldBe HttpStatusCode.Unauthorized
    }
})