package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.graphql.ChatExistsException
import com.neelkamath.omniChat.graphql.InvalidUserIdException
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val CREATE_PRIVATE_CHAT_QUERY: String = """
    mutation CreatePrivateChat(${"$"}userId: ID!) {
        createPrivateChat(userId: ${"$"}userId)
    }
"""

private fun operateCreatePrivateChat(userId: String, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(CREATE_PRIVATE_CHAT_QUERY, variables = mapOf("userId" to userId), accessToken = accessToken)

fun createPrivateChat(userId: String, accessToken: String): Int =
    operateCreatePrivateChat(userId, accessToken).data!!["createPrivateChat"] as Int

fun errCreatePrivateChat(userId: String, accessToken: String): String =
    operateCreatePrivateChat(userId, accessToken).errors!![0].message

class CreatePrivateChatTest : FunSpec({
    test("A chat should be created") {
        val (user1, user2) = createVerifiedUsers(2)
        val chatId = createPrivateChat(user2.info.id, user1.accessToken)
        PrivateChats.readIdList(user1.info.id) shouldBe listOf(chatId)
    }

    test("An existing chat shouldn't be recreated") {
        val (user1, user2) = createVerifiedUsers(2)
        createPrivateChat(user2.info.id, user1.accessToken)
        errCreatePrivateChat(user2.info.id, user1.accessToken) shouldBe ChatExistsException.message
    }

    test("A chat shouldn't be created with a nonexistent user") {
        val token = createVerifiedUsers(1)[0].accessToken
        errCreatePrivateChat("a nonexistent user ID", token) shouldBe InvalidUserIdException.message
    }

    test("A chat shouldn't be created with the user themselves") {
        val user = createVerifiedUsers(1)[0]
        errCreatePrivateChat(user.info.id, user.accessToken) shouldBe InvalidUserIdException.message
    }
})