package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.PrivateChatDeletions
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

const val CREATE_MESSAGE_QUERY = """
    mutation CreateMessage(${"$"}chatId: Int!, ${"$"}text: TextMessage!) {
        createMessage(chatId: ${"$"}chatId, text: ${"$"}text)
    }
"""

private fun operateCreateMessage(accessToken: String, chatId: Int, message: TextMessage): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        CREATE_MESSAGE_QUERY,
        variables = mapOf("chatId" to chatId, "text" to message),
        accessToken = accessToken
    )

fun createMessage(accessToken: String, chatId: Int, message: TextMessage): Placeholder {
    val data = operateCreateMessage(accessToken, chatId, message).data!!["createMessage"] as String
    return objectMapper.convertValue(data)
}

fun errCreateMessage(accessToken: String, chatId: Int, message: TextMessage): String =
    operateCreateMessage(accessToken, chatId, message).errors!![0].message

class CreateMessageTest : FunSpec({
    /** Asserts that [messages] has exactly one [MessageEdge], which is a [text] sent by the [userId]. */
    fun testMessages(messages: List<MessageEdge>, userId: String, text: TextMessage) {
        messages shouldHaveSize 1
        with(messages[0].node) {
            sender.id shouldBe userId
            this.text shouldBe text
        }
    }

    test("A message should be sent in a private chat") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val message = TextMessage("Hi")
        createMessage(user1.accessToken, chatId, message)
        testMessages(Messages.readPrivateChat(chatId, user1.info.id), user1.info.id, message)
    }

    test("A message should be sent in a group chat") {
        val user = createVerifiedUsers(1)[0]
        val chatId = GroupChats.create(user.info.id, buildNewGroupChat())
        val text = TextMessage("Hi")
        createMessage(user.accessToken, chatId, text)
        testMessages(Messages.readGroupChat(chatId), user.info.id, text)
    }

    test("Messaging in a chat the user isn't in should throw an exception") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = GroupChats.create(user1.info.id, buildNewGroupChat())
        GroupChats.create(user1.info.id, buildNewGroupChat())
        errCreateMessage(user2.accessToken, chatId, TextMessage("t")) shouldBe InvalidChatIdException.message
    }

    test("The user should be able to create a message in a private chat they just deleted") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        PrivateChatDeletions.create(chatId, user1.info.id)
        createMessage(user1.accessToken, chatId, TextMessage("t"))
    }
})