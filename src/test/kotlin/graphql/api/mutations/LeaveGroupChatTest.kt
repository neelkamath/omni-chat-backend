package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidNewAdminIdException
import com.neelkamath.omniChat.graphql.MissingNewAdminIdException
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.subscriptions.receiveMessageUpdates
import com.neelkamath.omniChat.test.graphql.createSignedInUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val LEAVE_GROUP_CHAT_QUERY: String = """
    mutation LeaveGroupChat(${"$"}chatId: Int!, ${"$"}newAdminId: ID) {
        leaveGroupChat(chatId: ${"$"}chatId, newAdminId: ${"$"}newAdminId)
    }
"""

private fun operateLeaveGroupChat(accessToken: String, chatId: Int, newAdminId: String? = null): GraphQlResponse =
    operateQueryOrMutation(
        LEAVE_GROUP_CHAT_QUERY,
        variables = mapOf("chatId" to chatId, "newAdminId" to newAdminId),
        accessToken = accessToken
    )

fun leaveGroupChat(accessToken: String, chatId: Int, newAdminId: String? = null): Boolean =
    operateLeaveGroupChat(accessToken, chatId, newAdminId).data!!["leaveGroupChat"] as Boolean

fun errLeaveGroupChat(accessToken: String, chatId: Int, newAdminId: String? = null): String =
    operateLeaveGroupChat(accessToken, chatId, newAdminId).errors!![0].message

class LeaveGroupChatTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    test("A non-admin should leave the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        leaveGroupChat(user.accessToken, chatId)
        GroupChatUsers.readUserIdList(chatId) shouldBe listOf(admin.info.id)
    }

    test("The admin should leave the chat after specifying the new admin if there are users left in the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        leaveGroupChat(admin.accessToken, chatId, newAdminId = user.info.id)
        GroupChatUsers.readUserIdList(chatId) shouldBe listOf(user.info.id)
    }

    test("The admin should leave the chat without specifying a new admin if they are the only user") {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        leaveGroupChat(token, chatId)
    }

    fun testBadUserId(supplyingId: Boolean) {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        val newAdminId = if (supplyingId) "invalid new admin ID" else null
        val exception = if (supplyingId) InvalidNewAdminIdException else MissingNewAdminIdException
        errLeaveGroupChat(admin.accessToken, chatId, newAdminId) shouldBe exception.message
    }

    test("The admin shouldn't be allowed to leave without specifying a new admin if there are users left") {
        testBadUserId(supplyingId = false)
    }

    test("The admin shouldn't be allowed to leave the chat if the new admin's user ID is invalid") {
        testBadUserId(supplyingId = true)
    }

    test("Leaving a group chat the user is not in should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errLeaveGroupChat(token, chatId = 1) shouldBe InvalidChatIdException.message
    }

    test("The user should be unsubscribed from the chat's message updates when they leave the chat") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        receiveMessageUpdates(user.accessToken, chatId) { incoming, _ ->
            leaveGroupChat(user.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }

    test(
        """
        Given a non-admin leaving the chat,
        when they specify a new admin,
        then the admin shouldn't be changed
        """
    ) {
        val (user1, user2, user3) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user2.info.id, user3.info.id))
        val chatId = createGroupChat(user1.accessToken, chat)
        leaveGroupChat(user2.accessToken, chatId, user3.info.id)
        GroupChats.isAdmin(user1.info.id, chatId).shouldBeTrue()
    }
}