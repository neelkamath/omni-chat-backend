package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.MessageEdge
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidMessageLengthException
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

const val CREATE_MESSAGE_QUERY: String = """
    mutation CreateMessage(${"$"}chatId: Int!, ${"$"}text: String!) {
        createMessage(chatId: ${"$"}chatId, text: ${"$"}text)
    }
"""

private fun operateCreateMessage(accessToken: String, chatId: Int, text: String): GraphQlResponse =
    operateQueryOrMutation(
        CREATE_MESSAGE_QUERY,
        variables = mapOf("chatId" to chatId, "text" to text),
        accessToken = accessToken
    )

fun createMessage(accessToken: String, chatId: Int, text: String): Boolean =
    operateCreateMessage(accessToken, chatId, text).data!!["createMessage"] as Boolean

fun errCreateMessage(accessToken: String, chatId: Int, text: String): String =
    operateCreateMessage(accessToken, chatId, text).errors!![0].message

class CreateMessageTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    /** Asserts that [messages] has exactly one [MessageEdge], which is a [text] sent by the [userId]. */
    fun testMessages(messages: List<MessageEdge>, userId: String, text: String) {
        messages shouldHaveSize 1
        with(messages[0].node) {
            sender.id shouldBe userId
            this.text shouldBe text
        }
    }

    test("A message should be sent in a private chat") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val text = "Hi"
        createMessage(user1.accessToken, chatId, text)
        testMessages(Messages.readPrivateChat(chatId, user1.info.id), user1.info.id, text)
    }

    test("A message should be sent in a group chat") {
        val user = createSignedInUsers(1)[0]
        val chatId = createGroupChat(user.accessToken, NewGroupChat("Title"))
        val text = "Hi"
        createMessage(user.accessToken, chatId, text)
        testMessages(Messages.readGroupChat(chatId), user.info.id, text)
    }

    test("Messaging in a chat the user isn't in should throw an exception") {
        val (user1, user2) = createSignedInUsers(2)
        val chat1Id = createGroupChat(user1.accessToken, NewGroupChat("Title"))
        createGroupChat(user2.accessToken, NewGroupChat("Title"))
        errCreateMessage(user2.accessToken, chat1Id, "message") shouldBe InvalidChatIdException.message
    }

    test("Sending a message longer than 10,000 characters should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        val message = CharArray(Messages.MAX_TEXT_LENGTH + 1) { 'a' }.joinToString("")
        errCreateMessage(token, chatId, message) shouldBe InvalidMessageLengthException.message
    }

    test("The user should be able to create a message in a private chat they just deleted") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        createMessage(user1.accessToken, chatId, "text")
    }
}