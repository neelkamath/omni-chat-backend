package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChatWithId
import com.neelkamath.omniChat.db.GroupChats
import io.kotest.core.spec.style.StringSpec
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

fun updateGroupChat(update: GroupChatUpdate, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "group-chat") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(update))
        }
    }.response

fun leaveGroupChat(jwt: String, chatId: Int, newAdminUserId: String? = null): TestApplicationResponse =
    withTestApplication(Application::main) {
        val parameters = Parameters.build {
            append("chat_id", chatId.toString())
            newAdminUserId?.let { append("new_admin_user_id", it) }
        }.formUrlEncode()
        handleRequest(HttpMethod.Delete, "group-chat?$parameters") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
        }
    }.response

class PatchGroupChatTest : StringSpec({
    listener(AppListener())

    "A group chat should update" {
        val users = createVerifiedUsers(3)
        val creator = users[0]
        val jwt = getJwt(creator.login)
        val initialUserIdList = setOf(users[1].id)
        val createdChatResponse = createGroupChat(GroupChat(initialUserIdList, "Title"), jwt)
        val chatId = gson.fromJson(createdChatResponse.content, ChatId::class.java).id
        val update = GroupChatUpdate(
            chatId,
            "New Title",
            "New description",
            newUserIdList = setOf(users[2].id),
            removedUserIdList = setOf(users[1].id)
        )
        val response = updateGroupChat(update, jwt)
        response.status() shouldBe HttpStatusCode.NoContent
        val userIdList = initialUserIdList + update.newUserIdList!! - update.removedUserIdList!!
        val groupChat = GroupChat(userIdList + creator.id, update.title!!, update.description)
        GroupChats.read(creator.id) shouldBe listOf(GroupChatWithId(chatId, groupChat, isAdmin = true))
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
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        GroupChats.read(creator.id) shouldBe listOf(GroupChatWithId(chatId, chat, isAdmin = true))
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

class DeleteGroupChatTest : StringSpec({
    listener(AppListener())

    "A non-admin should leave the chat" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = GroupChat(setOf(user.id), "Title")
        val response = createGroupChat(chat, getJwt(admin.login))
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        leaveGroupChat(getJwt(user.login), chatId).status() shouldBe HttpStatusCode.NoContent
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(admin.id)
    }

    "The admin should leave the chat after specifying the new admin if there are users left in the chat" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = GroupChat(setOf(user.id), "Title")
        val response = createGroupChat(chat, getJwt(admin.login))
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        leaveGroupChat(getJwt(admin.login), chatId, newAdminUserId = user.id).status() shouldBe HttpStatusCode.NoContent
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(user.id)
    }

    "The admin should leave the chat without specifying a new admin if they are the last user" {
        val (admin, user) = createVerifiedUsers(2)
        val adminJwt = getJwt(admin.login)
        val response = createGroupChat(GroupChat(setOf(admin.id, user.id), "Title"), adminJwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        leaveGroupChat(getJwt(user.login), chatId)
        leaveGroupChat(adminJwt, chatId).status() shouldBe HttpStatusCode.NoContent
    }

    fun testBadResponse(response: TestApplicationResponse, reason: InvalidGroupLeaveReason) {
        response.status() shouldBe HttpStatusCode.BadRequest
        gson.fromJson(response.content, InvalidGroupLeave::class.java) shouldBe InvalidGroupLeave(reason)
    }

    fun testBadUserId(givingId: Boolean) {
        val (admin, user) = createVerifiedUsers(2)
        val jwt = getJwt(admin.login)
        val response = createGroupChat(GroupChat(setOf(user.id), "Title"), jwt)
        val chatId = gson.fromJson(response.content, ChatId::class.java).id
        val newAdminUserId = if (givingId) "invalid new admin ID" else null
        val reason =
            if (givingId) InvalidGroupLeaveReason.INVALID_NEW_ADMIN_ID else InvalidGroupLeaveReason.MISSING_NEW_ADMIN_ID
        testBadResponse(leaveGroupChat(jwt, chatId, newAdminUserId), reason)
    }

    "The admin shouldn't be allowed to leave without specifying a new admin if there are users left" {
        testBadUserId(givingId = false)
    }

    "The admin shouldn't be allowed to leave the chat if the new admin's user ID is invalid" {
        testBadUserId(givingId = true)
    }

    "Leaving a group chat the user is not in should respond with an HTTP status code of 400" {
        val jwt = getJwt(createVerifiedUsers(1)[0].login)
        val response = leaveGroupChat(jwt, chatId = 1)
        testBadResponse(response, InvalidGroupLeaveReason.INVALID_CHAT_ID)
    }
})