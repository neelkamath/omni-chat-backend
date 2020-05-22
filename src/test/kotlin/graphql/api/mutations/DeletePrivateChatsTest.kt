package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.PrivateChatDeletions
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.subscriptions.operateMessageUpdates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val DELETE_PRIVATE_CHAT_QUERY: String = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId)
    }
"""

fun errDeletePrivateChat(chatId: Int, accessToken: String): String =
    operateDeletePrivateChat(chatId, accessToken).errors!![0].message

private fun operateDeletePrivateChat(chatId: Int, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(DELETE_PRIVATE_CHAT_QUERY, variables = mapOf("chatId" to chatId), accessToken = accessToken)

fun deletePrivateChat(chatId: Int, accessToken: String): Boolean =
    operateDeletePrivateChat(chatId, accessToken).data!!["deletePrivateChat"] as Boolean

class DeletePrivateChatTest : FunSpec({
    listener(AppListener())

    test("A chat should be deleted") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        deletePrivateChat(chatId, user1.accessToken)
        PrivateChatDeletions.isDeleted(user1.info.id, chatId).shouldBeTrue()
    }

    test("Deleting an invalid chat ID should throw an exception") {
        val token = createVerifiedUsers(1)[0].accessToken
        errDeletePrivateChat(chatId = 1, accessToken = token) shouldBe InvalidChatIdException().message
    }

    test("Deleting a chat should unsubscribe the user from its message updates") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        operateMessageUpdates(chatId, user1.accessToken) { incoming, _ ->
            deletePrivateChat(chatId, user1.accessToken)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})