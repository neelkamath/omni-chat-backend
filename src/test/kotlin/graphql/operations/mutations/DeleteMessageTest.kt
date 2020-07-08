package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val DELETE_MESSAGE_QUERY = """
    mutation DeleteMessage(${"$"}id: Int!) {
        deleteMessage(id: ${"$"}id)
    }
"""

private fun operateDeleteMessage(accessToken: String, messageId: Int): GraphQlResponse =
    operateGraphQlQueryOrMutation(DELETE_MESSAGE_QUERY, variables = mapOf("id" to messageId), accessToken = accessToken)

fun deleteMessage(accessToken: String, messageId: Int): Placeholder {
    val data = operateDeleteMessage(accessToken, messageId).data!!["deleteMessage"] as String
    return objectMapper.convertValue(data)
}

fun errDeleteMessage(accessToken: String, messageId: Int): String =
    operateDeleteMessage(accessToken, messageId).errors!![0].message

class DeleteMessageTest : FunSpec({
    test("The user's message should be deleted") {
        val admin = createVerifiedUsers(1)[0]
        val chatId = GroupChats.create(admin.info.id, buildNewGroupChat())
        val messageId = Messages.message(chatId, admin.info.id, TextMessage("t"))
        deleteMessage(admin.accessToken, messageId)
        Messages.readGroupChat(chatId).shouldBeEmpty()
    }

    test("Deleting a nonexistent message should return an error") {
        val token = createVerifiedUsers(1)[0].accessToken
        errDeleteMessage(token, messageId = 0) shouldBe InvalidMessageIdException.message
    }

    test("Deleting a message from a chat the user isn't in should throw an exception") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = GroupChats.create(user2.info.id, buildNewGroupChat())
        val messageId = Messages.message(chatId, user2.info.id, TextMessage("t"))
        errDeleteMessage(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }

    test("Deleting another user's message should return an error") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val messageId = Messages.message(chatId, user2.info.id, TextMessage("t"))
        errDeleteMessage(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }

    test(
        """
        Given a user who created a private chat, sent a message, and deleted the chat,
        when deleting the message,
        then it should fail
        """
    ) {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val messageId = Messages.message(chatId, user1.info.id, TextMessage("t"))
        PrivateChatDeletions.create(chatId, user1.info.id)
        errDeleteMessage(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }
})