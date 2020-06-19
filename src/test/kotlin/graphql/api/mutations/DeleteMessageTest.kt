package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.graphql.api.messageAndReadId
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val DELETE_MESSAGE_QUERY: String = """
    mutation DeleteMessage(${"$"}id: Int!, ${"$"}chatId: Int!) {
        deleteMessage(id: ${"$"}id, chatId: ${"$"}chatId)
    }
"""

private fun operateDeleteMessage(accessToken: String, id: Int, chatId: Int): GraphQlResponse = operateQueryOrMutation(
    DELETE_MESSAGE_QUERY,
    variables = mapOf("id" to id, "chatId" to chatId),
    accessToken = accessToken
)

fun deleteMessage(accessToken: String, id: Int, chatId: Int): Boolean =
    operateDeleteMessage(accessToken, id, chatId).data!!["deleteMessage"] as Boolean

fun errDeleteMessage(accessToken: String, id: Int, chatId: Int): String =
    operateDeleteMessage(accessToken, id, chatId).errors!![0].message

class DeleteMessageTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("""Deleting the user's message should return "true"""") {
        val admin = createSignedInUsers(1)[0]
        val chatId = createGroupChat(admin.accessToken, NewGroupChat("Title"))
        val messageId = messageAndReadId(
            admin.accessToken,
            chatId,
            "text"
        )
        deleteMessage(admin.accessToken, messageId, chatId).shouldBeTrue()
        Messages.readGroupChat(chatId).shouldBeEmpty()
    }

    test("Deleting another user's message should return an error") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val messageId = messageAndReadId(
            user2.accessToken,
            chatId,
            "text"
        )
        errDeleteMessage(user1.accessToken, messageId, chatId) shouldBe InvalidMessageIdException.message
    }

    test("Deleting a nonexistent message should return an error") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        errDeleteMessage(id = 0, chatId = chatId, accessToken = token) shouldBe InvalidMessageIdException.message
    }

    test("Deleting a message from a nonexistent chat should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errDeleteMessage(id = 0, chatId = 0, accessToken = token) shouldBe InvalidChatIdException.message
    }

    test(
        """
        Given a user who created a private chat, sent a message, and deleted the chat,
        when deleting the message,
        then it should fail
        """
    ) {
        val (user1, user2) = createSignedInUsers(2)
        val create = { createPrivateChat(user1.accessToken, user2.info.id) }
        val chatId = create()
        val messageId = messageAndReadId(
            user1.accessToken,
            chatId,
            "text"
        )
        deletePrivateChat(user1.accessToken, chatId)
        create()
        errDeleteMessage(user1.accessToken, messageId, chatId) shouldBe InvalidMessageIdException.message
    }
}