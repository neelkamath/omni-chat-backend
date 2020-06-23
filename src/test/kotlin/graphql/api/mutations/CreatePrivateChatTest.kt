package com.neelkamath.omniChat.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.graphql.ChatExistsException
import com.neelkamath.omniChat.graphql.InvalidUserIdException
import com.neelkamath.omniChat.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_PRIVATE_CHAT_QUERY: String = """
    mutation CreatePrivateChat(${"$"}userId: ID!) {
        createPrivateChat(userId: ${"$"}userId)
    }
"""

private fun operateCreatePrivateChat(accessToken: String, userId: String): GraphQlResponse = operateQueryOrMutation(
    CREATE_PRIVATE_CHAT_QUERY,
    variables = mapOf("userId" to userId),
    accessToken = accessToken
)

fun createPrivateChat(accessToken: String, userId: String): Int =
    operateCreatePrivateChat(accessToken, userId).data!!["createPrivateChat"] as Int

fun errCreatePrivateChat(accessToken: String, userId: String): String =
    operateCreatePrivateChat(accessToken, userId).errors!![0].message

class CreatePrivateChatTest : FunSpec({
    test("A chat should be created") {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        PrivateChats.readIdList(user1.info.id) shouldBe listOf(chatId)
    }

    test("Attempting to create a chat the user is in should return an error") {
        val (user1, user2) = createSignedInUsers(2)
        createPrivateChat(user1.accessToken, user2.info.id)
        errCreatePrivateChat(user1.accessToken, user2.info.id) shouldBe ChatExistsException.message
    }

    test(
        """
        Given a chat which was deleted by the user,
        when the user recreates the chat,
        then the existing chat's ID should be received
        """
    ) {
        val (user1, user2) = createSignedInUsers(2)
        val chatId = createPrivateChat(user1.accessToken, user2.info.id)
        deletePrivateChat(user1.accessToken, chatId)
        createPrivateChat(user1.accessToken, user2.info.id) shouldBe chatId
    }

    test("A chat shouldn't be created with a nonexistent user") {
        val token = createSignedInUsers(1)[0].accessToken
        errCreatePrivateChat(token, "a nonexistent user ID") shouldBe InvalidUserIdException.message
    }

    test("A chat shouldn't be created with the user themselves") {
        val user = createSignedInUsers(1)[0]
        errCreatePrivateChat(user.accessToken, user.info.id) shouldBe InvalidUserIdException.message
    }
})