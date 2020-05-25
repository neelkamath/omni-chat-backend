package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.db.MessageStatuses
import com.neelkamath.omniChat.graphql.DuplicateStatusException
import com.neelkamath.omniChat.graphql.InvalidMessageIdException
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

const val CREATE_READ_STATUS_QUERY: String = """
    mutation CreateReadStatus(${"$"}messageId: Int!) {
        createReadStatus(messageId: ${"$"}messageId)
    }
"""

private fun operateCreateReadStatus(messageId: Int, accessToken: String): GraphQlResponse = operateQueryOrMutation(
    CREATE_READ_STATUS_QUERY,
    variables = mapOf("messageId" to messageId),
    accessToken = accessToken
)

fun createReadStatus(messageId: Int, accessToken: String): Boolean =
    operateCreateReadStatus(messageId, accessToken).data!!["createReadStatus"] as Boolean

fun errCreateReadStatus(messageId: Int, accessToken: String): String =
    operateCreateReadStatus(messageId, accessToken).errors!![0].message

class CreateReadStatusTest : FunSpec({
    test("""Creating a status should return "true"""") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createReadStatus(messageId, user1.accessToken).shouldBeTrue()
    }

    test(
        """
        Given a message which has neither a "delivered" nor a "read" status,
        when a "read" status is created for the message,
        then both a "delivered" and "read" status should be created for the message
        """
    ) {
        val (messageId, user1) = createUtilizedPrivateChat()
        createReadStatus(messageId, user1.accessToken)
        MessageStatuses.read(messageId).map { it.status } shouldBe listOf(MessageStatus.DELIVERED, MessageStatus.READ)
    }

    test("Creating a duplicate status should fail") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createReadStatus(messageId, user1.accessToken)
        errCreateReadStatus(messageId, user1.accessToken) shouldBe DuplicateStatusException.message
    }

    test("Creating a status on a message from a chat the user isn't in should fail") {
        val (messageId) = createUtilizedPrivateChat()
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateReadStatus(messageId, token) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on a nonexistent message should fail") {
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateReadStatus(messageId = 1, accessToken = token) shouldBe InvalidMessageIdException.message
    }

    test("Creating a status on the user's own message should fail") {
        val (messageId, _, user2) = createUtilizedPrivateChat()
        errCreateReadStatus(messageId, user2.accessToken) shouldBe InvalidMessageIdException.message
    }
})