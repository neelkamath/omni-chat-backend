package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.MessageStatuses
import com.neelkamath.omniChat.graphql.DuplicateStatusException
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.messageAndReadId
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

const val CREATE_DELIVERED_STATUS_QUERY = """
    mutation CreateDeliveredStatus(${"$"}messageId: Int!) {
        createDeliveredStatus(messageId: ${"$"}messageId)
    }
"""

private fun operateCreateDeliveredStatus(accessToken: String, messageId: Int): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        CREATE_DELIVERED_STATUS_QUERY,
        variables = mapOf("messageId" to messageId),
        accessToken = accessToken
    )

fun createDeliveredStatus(accessToken: String, messageId: Int): Placeholder {
    val data = operateCreateDeliveredStatus(accessToken, messageId).data!!["createDeliveredStatus"] as String
    return objectMapper.convertValue(data)
}

fun errCreateDeliveredStatus(accessToken: String, messageId: Int): String =
    operateCreateDeliveredStatus(accessToken, messageId).errors!![0].message

class CreateDeliveredStatusTest : FunSpec({
    test("A status should be created") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createDeliveredStatus(user1.accessToken, messageId)
        val statuses = MessageStatuses.read(messageId)
        statuses shouldHaveSize 1
        statuses[0].status shouldBe MessageStatus.DELIVERED
    }

    test("Creating a duplicate status should fail") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createDeliveredStatus(user1.accessToken, messageId)
        errCreateDeliveredStatus(user1.accessToken, messageId) shouldBe DuplicateStatusException.message
    }

    test("Creating a status on the user's own message should fail") {
        val (messageId, _, user2) = createUtilizedPrivateChat()
        errCreateDeliveredStatus(user2.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on a message from a chat the user isn't in should fail") {
        val (messageId) = createUtilizedPrivateChat()
        val token = createSignedInUsers(1)[0].accessToken
        errCreateDeliveredStatus(token, messageId) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on a nonexistent message should fail") {
        val token = createSignedInUsers(1)[0].accessToken
        errCreateDeliveredStatus(messageId = 1, accessToken = token) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status in a private chat the user deleted should fail") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        val messageId = messageAndReadId(user1.accessToken, chatId, TextMessage("t"))
        deletePrivateChat(user1.accessToken, chatId)
        errCreateDeliveredStatus(user1.accessToken, messageId) shouldBe InvalidMessageIdException.message
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
        val messageId = messageAndReadId(user1.accessToken, chatId, TextMessage("t"))
        deletePrivateChat(user2.accessToken, chatId)
        errCreateDeliveredStatus(user2.accessToken, messageId) shouldBe InvalidMessageIdException.message
    }
})