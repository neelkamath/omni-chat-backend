package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.findUserById
import com.neelkamath.omniChat.graphql.InvalidDescriptionLengthException
import com.neelkamath.omniChat.graphql.InvalidTitleLengthException
import com.neelkamath.omniChat.graphql.InvalidUserIdException
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_GROUP_CHAT_QUERY: String = """
    mutation CreateGroupChat(${"$"}chat: NewGroupChat!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

private fun operateCreateGroupChat(accessToken: String, chat: NewGroupChat): GraphQlResponse =
    operateQueryOrMutation(CREATE_GROUP_CHAT_QUERY, variables = mapOf("chat" to chat), accessToken = accessToken)

fun createGroupChat(accessToken: String, chat: NewGroupChat): Int =
    operateCreateGroupChat(accessToken, chat).data!!["createGroupChat"] as Int

fun errCreateGroupChat(accessToken: String, chat: NewGroupChat): String =
    operateCreateGroupChat(accessToken, chat).errors!![0].message

class CreateGroupChatTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A group chat should be created, ignoring the user's own ID") {
        val (admin, user1, user2) = createSignedInUsers(3)
        val chat = NewGroupChat("Title", "Description", listOf(admin.info.id, user1.info.id, user2.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        GroupChats.readUserChats(admin.info.id) shouldBe listOf(
            GroupChat(
                chatId,
                admin.info.id,
                chat.userIdList.map(::findUserById),
                chat.title,
                chat.description,
                Messages.readGroupChatConnection(chatId)
            )
        )
    }

    test("A group chat should not be created when supplied with an invalid user ID") {
        val chat = NewGroupChat("Title", userIdList = listOf("invalid user ID"))
        val token = createSignedInUsers(1)[0].accessToken
        errCreateGroupChat(token, chat) shouldBe InvalidUserIdException.message
    }

    test("A group chat should not be created if an empty title is supplied") {
        val chat = NewGroupChat(title = "")
        val token = createSignedInUsers(1)[0].accessToken
        errCreateGroupChat(token, chat) shouldBe InvalidTitleLengthException.message
    }

    test("A group chat should not be created if the title is too long") {
        val title = CharArray(GroupChats.MAX_TITLE_LENGTH + 1) { 'a' }.joinToString("")
        val chat = NewGroupChat(title)
        val token = createSignedInUsers(1)[0].accessToken
        errCreateGroupChat(token, chat) shouldBe InvalidTitleLengthException.message
    }

    test("A group chat should not be created if the description has an invalid length") {
        val description = CharArray(GroupChats.MAX_DESCRIPTION_LENGTH + 1) { 'a' }.joinToString("")
        val chat = NewGroupChat("Title", description)
        val token = createSignedInUsers(1)[0].accessToken
        errCreateGroupChat(token, chat) shouldBe InvalidDescriptionLengthException.message
    }
}