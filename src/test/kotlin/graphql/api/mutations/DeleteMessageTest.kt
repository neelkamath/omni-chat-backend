package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

const val DELETE_MESSAGE_QUERY: String = """
    mutation DeleteMessage(${"$"}id: Int!, ${"$"}chatId: Int!) {
        deleteMessage(id: ${"$"}id, chatId: ${"$"}chatId)
    }
"""

private fun operateDeleteMessage(id: Int, chatId: Int, accessToken: String): GraphQlResponse = operateQueryOrMutation(
    DELETE_MESSAGE_QUERY,
    variables = mapOf("id" to id, "chatId" to chatId),
    accessToken = accessToken
)

fun deleteMessage(id: Int, chatId: Int, accessToken: String): Boolean =
    operateDeleteMessage(id, chatId, accessToken).data!!["deleteMessage"] as Boolean

fun errDeleteMessage(id: Int, chatId: Int, accessToken: String): String =
    operateDeleteMessage(id, chatId, accessToken).errors!![0].message

class DeleteMessageTest : FunSpec({
    test("Deleting the user's message should return true") {
        val admin = createVerifiedUsers(1)[0]
        val chatId = createGroupChat(NewGroupChat("Title"), admin.accessToken)
        val messageId = messageAndReadId(chatId, "text", admin.accessToken)
        deleteMessage(messageId, chatId, admin.accessToken).shouldBeTrue()
        Messages.readChat(chatId).shouldBeEmpty()
    }

    test("Deleting another user's message should return an error") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        val messageId = messageAndReadId(chatId, "text", user2.accessToken)
        errDeleteMessage(messageId, chatId, user1.accessToken) shouldBe InvalidMessageIdException.message
    }

    test("Deleting a nonexistent message should return an error") {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        errDeleteMessage(id = 0, chatId = chatId, accessToken = token) shouldBe InvalidMessageIdException.message
    }

    test("Deleting a message from a nonexistent chat should throw an exception") {
        val token = createVerifiedUsers(1)[0].accessToken
        errDeleteMessage(id = 0, chatId = 0, accessToken = token) shouldBe InvalidChatIdException.message
    }
})