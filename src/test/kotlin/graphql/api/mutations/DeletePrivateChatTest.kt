package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.PrivateChatDeletions
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.subscriptions.receiveMessageUpdates
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val DELETE_PRIVATE_CHAT_QUERY: String = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId)
    }
"""

private fun operateDeletePrivateChat(accessToken: String, chatId: Int): GraphQlResponse = operateQueryOrMutation(
    DELETE_PRIVATE_CHAT_QUERY,
    variables = mapOf("chatId" to chatId),
    accessToken = accessToken
)

fun deletePrivateChat(accessToken: String, chatId: Int): Boolean =
    operateDeletePrivateChat(accessToken, chatId).data!!["deletePrivateChat"] as Boolean

fun errDeletePrivateChat(accessToken: String, chatId: Int): String =
    operateDeletePrivateChat(accessToken, chatId).errors!![0].message

class DeletePrivateChatTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A chat should be deleted") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        PrivateChatDeletions.isDeleted(user1.info.id, chatId).shouldBeTrue()
    }

    test("Deleting an invalid chat ID should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errDeletePrivateChat(chatId = 1, accessToken = token) shouldBe InvalidChatIdException.message
    }

    test("Deleting a chat should unsubscribe the user from its message updates") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        receiveMessageUpdates(user1.accessToken, chatId) { incoming, _ ->
            deletePrivateChat(user1.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test("Deleting a deleted chat which still exists shouldn't fail") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        repeat(2) { deletePrivateChat(user1.accessToken, chatId) }
    }
}