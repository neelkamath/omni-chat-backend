package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.Placeholder
import com.neelkamath.omniChat.db.tables.PrivateChatDeletions
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

const val DELETE_PRIVATE_CHAT_QUERY = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId)
    }
"""

private fun operateDeletePrivateChat(accessToken: String, chatId: Int): GraphQlResponse = operateGraphQlQueryOrMutation(
    DELETE_PRIVATE_CHAT_QUERY,
    variables = mapOf("chatId" to chatId),
    accessToken = accessToken
)

fun deletePrivateChat(accessToken: String, chatId: Int): Placeholder {
    val data = operateDeletePrivateChat(accessToken, chatId).data!!["deletePrivateChat"] as String
    return objectMapper.convertValue(data)
}

fun errDeletePrivateChat(accessToken: String, chatId: Int): String =
    operateDeletePrivateChat(accessToken, chatId).errors!![0].message

class DeletePrivateChatTest : FunSpec({
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

    test("Deleting a deleted chat which still exists shouldn't fail") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        repeat(2) { deletePrivateChat(user1.accessToken, chatId) }
    }
})