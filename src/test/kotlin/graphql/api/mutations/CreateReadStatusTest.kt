package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.db.MessageStatuses
import com.neelkamath.omniChat.graphql.DuplicateStatusException
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.test.graphql.api.createUtilizedPrivateChat
import com.neelkamath.omniChat.test.graphql.api.messageAndReadId
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

const val CREATE_READ_STATUS_QUERY: String = """
    mutation CreateReadStatus(${"$"}messageId: Int!) {
        createReadStatus(messageId: ${"$"}messageId)
    }
"""

private fun operateCreateReadStatus(accessToken: String, messageId: Int): GraphQlResponse = operateQueryOrMutation(
    CREATE_READ_STATUS_QUERY,
    variables = mapOf("messageId" to messageId),
    accessToken = accessToken
)

fun createReadStatus(accessToken: String, messageId: Int): Boolean =
    operateCreateReadStatus(accessToken, messageId).data!!["createReadStatus"] as Boolean

fun errCreateReadStatus(accessToken: String, messageId: Int): String =
    operateCreateReadStatus(accessToken, messageId).errors!![0].message

class CreateReadStatusTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("""Creating a status should return "true"""") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createReadStatus(user1.accessToken, messageId).shouldBeTrue()
    }

    test(
        """
        Given a message which has neither a "delivered" nor a "read" status,
        when a "read" status is created for the message,
        then both a "delivered" and "read" status should be created for the message
        """
    ) {
        val (messageId, user1) = createUtilizedPrivateChat()
        createReadStatus(user1.accessToken, messageId)
        MessageStatuses.read(messageId).map { it.status } shouldBe listOf(MessageStatus.DELIVERED, MessageStatus.READ)
    }

    test("Creating a duplicate status should fail") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createReadStatus(user1.accessToken, messageId)
        errCreateReadStatus(user1.accessToken, messageId) shouldBe DuplicateStatusException.message
    }

    test("Creating a status on a message from a chat the user isn't in should fail") {
        val (messageId) = createUtilizedPrivateChat()
        val token = createSignedInUsers(1)[0].accessToken
        errCreateReadStatus(token, messageId) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on a nonexistent message should fail") {
        val token = createSignedInUsers(1)[0].accessToken
        errCreateReadStatus(messageId = 1, accessToken = token) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on the user's own message should fail") {
        val (messageId, _, user2) = createUtilizedPrivateChat()
        errCreateReadStatus(user2.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status in a private chat the user deleted should fail") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val messageId = messageAndReadId(
            user1.accessToken,
            chatId,
            "text"
        )
        deletePrivateChat(user1.accessToken, chatId)
        errCreateReadStatus(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }

    test(
        """
        Given a private chat in which the first user sent a message, and the second user deleted the chat,
        when the second user creates a status on the message,
        then it should fail
        """
    ) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val messageId = messageAndReadId(
            user1.accessToken,
            chatId,
            "text"
        )
        deletePrivateChat(user2.accessToken, chatId)
        errCreateReadStatus(user2.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }
}