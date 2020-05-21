package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidDescriptionLengthException
import com.neelkamath.omniChat.graphql.InvalidTitleLengthException
import com.neelkamath.omniChat.graphql.InvalidUserIdException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_GROUP_CHAT_QUERY: String = """
    mutation CreateGroupChat(${"$"}chat: NewGroupChat!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

fun errCreateGroupChat(chat: NewGroupChat, accessToken: String): String =
    operateCreateGroupChat(chat, accessToken).errors!![0].message

private fun operateCreateGroupChat(chat: NewGroupChat, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(CREATE_GROUP_CHAT_QUERY, variables = mapOf("chat" to chat), accessToken = accessToken)

fun createGroupChat(chat: NewGroupChat, accessToken: String): Int =
    operateCreateGroupChat(chat, accessToken).data!!["createGroupChat"] as Int

class CreateGroupChatTest : FunSpec({
    listener(AppListener())

    test("A group chat should be created, ignoring the user's own ID") {
        val (admin, user1, user2) = createVerifiedUsers(3)
        val chat = NewGroupChat("Title", "Description", setOf(admin.info.id, user1.info.id, user2.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        val userIdList = chat.userIdList + admin.info.id
        GroupChats.read(admin.info.id) shouldBe listOf(
            GroupChat(chatId, admin.info.id, userIdList, chat.title, chat.description, Messages.readChat(chatId))
        )
    }

    test("A group chat should not be created when supplied with an invalid user ID") {
        val chat = NewGroupChat("Title", userIdList = setOf("invalid user ID"))
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateGroupChat(chat, token) shouldBe InvalidUserIdException().message
    }

    test("A group chat should not be created if an empty title is supplied") {
        val chat = NewGroupChat(title = "")
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateGroupChat(chat, token) shouldBe InvalidTitleLengthException().message
    }

    test("A group chat should not be created if the title is too long") {
        val title = CharArray(GroupChats.MAX_TITLE_LENGTH + 1) { 'a' }.joinToString("")
        val chat = NewGroupChat(title)
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateGroupChat(chat, token) shouldBe InvalidTitleLengthException().message
    }

    test("A group chat should not be created if the description has an invalid length") {
        val description = CharArray(GroupChats.MAX_DESCRIPTION_LENGTH + 1) { 'a' }.joinToString("")
        val chat = NewGroupChat("Title", description)
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateGroupChat(chat, token) shouldBe InvalidDescriptionLengthException().message
    }
})