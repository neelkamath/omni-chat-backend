package com.neelkamath.omniChat.graphql.operations.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.InvalidUserIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_GROUP_CHAT_QUERY = """
    mutation CreateGroupChat(${"$"}chat: NewGroupChat!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

private fun operateCreateGroupChat(accessToken: String, chat: NewGroupChat): GraphQlResponse =
    operateGraphQlQueryOrMutation(CREATE_GROUP_CHAT_QUERY, variables = mapOf("chat" to chat), accessToken = accessToken)

fun createGroupChat(accessToken: String, chat: NewGroupChat): Int =
    operateCreateGroupChat(accessToken, chat).data!!["createGroupChat"] as Int

fun errCreateGroupChat(accessToken: String, chat: NewGroupChat): String =
    operateCreateGroupChat(accessToken, chat).errors!![0].message

class CreateGroupChatTest : FunSpec({
    test("A group chat should be created, ignoring the user's own ID, and keeping whitespace intact") {
        val (admin, user1, user2) = createSignedInUsers(3)
        val chat = NewGroupChat(
            GroupChatTitle(" Title  "),
            GroupChatDescription("  Description "),
            listOf(admin.info.id, user1.info.id, user2.info.id)
        )
        val chatId = createGroupChat(admin.accessToken, chat)
        GroupChats.readUserChats(admin.info.id) shouldBe listOf(
            GroupChat(
                chatId,
                admin.info.id,
                GroupChatUsers.readUsers(chatId),
                chat.title,
                chat.description,
                Messages.readGroupChatConnection(chatId)
            )
        )
    }

    test("A group chat shouldn't be created when supplied with an invalid user ID") {
        val token = createSignedInUsers(1)[0].accessToken
        errCreateGroupChat(token, buildNewGroupChat("invalid user ID")) shouldBe InvalidUserIdException.message
    }
})