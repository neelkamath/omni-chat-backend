package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.MessageDateTimes
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidMessageLengthException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_MESSAGE_QUERY: String = """
    mutation CreateMessage(${"$"}chatId: Int!, ${"$"}text: String!) {
        createMessage(chatId: ${"$"}chatId, text: ${"$"}text)
    }
"""

fun errCreateMessage(chatId: Int, text: String, accessToken: String): String =
    operateCreateMessage(chatId, text, accessToken).errors!![0].message

private fun operateCreateMessage(chatId: Int, text: String, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(
        CREATE_MESSAGE_QUERY,
        variables = mapOf("chatId" to chatId, "text" to text),
        accessToken = accessToken
    )

fun createMessage(chatId: Int, text: String, accessToken: String): Boolean =
    operateCreateMessage(chatId, text, accessToken).data!!["createMessage"] as Boolean

class CreateMessageTest : FunSpec({
    listener(AppListener())

    test("A message should be sent in a private chat") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        val text = "Hi"
        createMessage(chatId, text, user1.accessToken)
        val createdMessage = Messages.readChat(chatId)[0]
        val dateTimes = MessageDateTimes(createdMessage.dateTimes.sent)
        val message = Message(createdMessage.id, user1.info.id, text, dateTimes)
        Messages.readChat(chatId) shouldBe listOf(message)
    }

    test("A message should be sent in a group chat") {
        val user = createVerifiedUsers(1)[0]
        val chatId = createGroupChat(NewGroupChat("Title"), user.accessToken)
        val text = "Hi"
        createMessage(chatId, text, user.accessToken)
        val createdMessage = Messages.readChat(chatId)[0]
        val groupChat = Message(createdMessage.id, user.info.id, text, MessageDateTimes(createdMessage.dateTimes.sent))
        Messages.readChat(chatId) shouldBe listOf(groupChat)
    }

    test("Messaging in a chat the user isn't in should throw an exception") {
        val (user1, user2) = createVerifiedUsers(2)
        val chat1Id = createGroupChat(NewGroupChat("Title"), user1.accessToken)
        createGroupChat(NewGroupChat("Title"), user2.accessToken)
        errCreateMessage(chat1Id, "message", user2.accessToken) shouldBe InvalidChatIdException().message
    }

    test("Sending a message longer than 10,000 characters should throw an exception") {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        val message = CharArray(Messages.MAX_TEXT_LENGTH + 1) { 'a' }.joinToString("")
        errCreateMessage(chatId, message, token) shouldBe InvalidMessageLengthException().message
    }
})