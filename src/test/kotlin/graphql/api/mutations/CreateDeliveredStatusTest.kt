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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

const val CREATE_DELIVERED_STATUS_QUERY: String = """
    mutation CreateDeliveredStatus(${"$"}messageId: Int!) {
        createDeliveredStatus(messageId: ${"$"}messageId)
    }
"""

private fun operateCreateDeliveredStatus(messageId: Int, accessToken: String): GraphQlResponse = operateQueryOrMutation(
    CREATE_DELIVERED_STATUS_QUERY,
    variables = mapOf("messageId" to messageId),
    accessToken = accessToken
)

fun createDeliveredStatus(messageId: Int, accessToken: String): Boolean =
    operateCreateDeliveredStatus(messageId, accessToken).data!!["createDeliveredStatus"] as Boolean

fun errCreateDeliveredStatus(messageId: Int, accessToken: String): String =
    operateCreateDeliveredStatus(messageId, accessToken).errors!![0].message

class CreateDeliveredStatusTest : FunSpec({
    test("""A status should be created returning "true"""") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createDeliveredStatus(messageId, user1.accessToken).shouldBeTrue()
        val statuses = MessageStatuses.read(messageId)
        statuses shouldHaveSize 1
        statuses[0].status shouldBe MessageStatus.DELIVERED
    }

    test("Creating a duplicate status should fail") {
        val (messageId, user1) = createUtilizedPrivateChat()
        createDeliveredStatus(messageId, user1.accessToken)
        errCreateDeliveredStatus(messageId, user1.accessToken) shouldBe DuplicateStatusException().message
    }

    test("Creating a status on the user's own message should fail") {
        val (messageId, _, user2) = createUtilizedPrivateChat()
        errCreateDeliveredStatus(messageId, user2.accessToken) shouldBe InvalidMessageIdException().message
    }

    test("Creating a status on a message from a chat the user isn't in should fail") {
        val (messageId) = createUtilizedPrivateChat()
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateDeliveredStatus(messageId, token) shouldBe InvalidMessageIdException().message
    }

    test("Creating a status on a nonexistent message should fail") {
        val token = createVerifiedUsers(1)[0].accessToken
        errCreateDeliveredStatus(messageId = 1, accessToken = token) shouldBe InvalidMessageIdException().message
    }
})