package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.DuplicateStatusException
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

const val CREATE_STATUS_QUERY = """
    mutation CreateStatus(${"$"}messageId: Int!, ${"$"}status: MessageStatus!) {
        createStatus(messageId: ${"$"}messageId, status: ${"$"}status)
    }
"""

private fun operateCreateStatus(accessToken: String, messageId: Int, status: MessageStatus): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        CREATE_STATUS_QUERY,
        variables = mapOf("messageId" to messageId, "status" to status),
        accessToken = accessToken
    )

fun createStatus(accessToken: String, messageId: Int, status: MessageStatus): Placeholder {
    val data = operateCreateStatus(accessToken, messageId, status).data!!["createStatus"] as String
    return objectMapper.convertValue(data)
}

fun errCreateStatus(accessToken: String, messageId: Int, status: MessageStatus): String =
    operateCreateStatus(accessToken, messageId, status).errors!![0].message

class CreateStatusTest : FunSpec({
    /** A private chat between two users where [user2] sent the [messageId]. */
    data class UtilizedPrivateChat(val messageId: Int, val user1: VerifiedUser, val user2: VerifiedUser)

    fun createUtilizedPrivateChat(): UtilizedPrivateChat {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val messageId = Messages.message(chatId, user2.info.id, TextMessage("t"))
        return UtilizedPrivateChat(messageId, user1, user2)
    }

    test("A delivered status should be created") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createStatus(user1.accessToken, messageId, MessageStatus.DELIVERED)
        val statuses = MessageStatuses.read(messageId)
        statuses shouldHaveSize 1
        statuses[0].status shouldBe MessageStatus.DELIVERED
    }

    test(
        """
        Given a message which has neither a "delivered" nor a "read" status,
        when a "read" status is created for the message,
        then both a "delivered" and "read" status should be created for the message
        """
    ) {
        val (messageId, user1) = createUtilizedPrivateChat()
        createStatus(user1.accessToken, messageId, MessageStatus.READ)
        MessageStatuses.read(messageId).map { it.status } shouldBe listOf(MessageStatus.DELIVERED, MessageStatus.READ)
    }

    test("Creating a duplicate status should fail") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createStatus(user1.accessToken, messageId, MessageStatus.DELIVERED)
        errCreateStatus(user1.accessToken, messageId, MessageStatus.DELIVERED) shouldBe DuplicateStatusException.message
    }

    test("Creating a status on the user's own message should fail") {
        val (messageId, _, user2) = createUtilizedPrivateChat()
        errCreateStatus(user2.accessToken, messageId, MessageStatus.DELIVERED) shouldBe
                InvalidMessageIdException.message
    }

    test("Creating a status on a message from a chat the user isn't in should fail") {
        val (messageId) = createUtilizedPrivateChat()
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateStatus(token, messageId, MessageStatus.DELIVERED) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on a nonexistent message should fail") {
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateStatus(token, messageId = 1, status = MessageStatus.DELIVERED) shouldBe
                InvalidMessageIdException.message
    }

    test("Creating a status in a private chat the user deleted should fail") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val messageId = Messages.message(chatId, user1.info.id, TextMessage("t"))
        PrivateChatDeletions.create(chatId, user1.info.id)
        errCreateStatus(user1.accessToken, messageId, MessageStatus.DELIVERED) shouldBe
                InvalidMessageIdException.message
    }

    test(
        """
        Given a private chat in which the first user sent a message, and the second user deleted the chat,
        when the second user creates a status on the message,
        then it should fail
        """
    ) {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = PrivateChats.create(user1.info.id, user2.info.id)
        val messageId = Messages.message(chatId, user1.info.id, TextMessage("t"))
        PrivateChatDeletions.create(chatId, user2.info.id)
        errCreateStatus(user2.accessToken, messageId, MessageStatus.DELIVERED) shouldBe
                InvalidMessageIdException.message
    }
})