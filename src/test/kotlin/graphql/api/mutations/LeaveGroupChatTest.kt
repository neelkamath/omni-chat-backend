package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidNewAdminIdException
import com.neelkamath.omniChat.graphql.MissingNewAdminIdException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import com.neelkamath.omniChat.test.graphql.api.subscriptions.operateMessageUpdates
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

const val LEAVE_GROUP_CHAT_QUERY: String = """
    mutation LeaveGroupChat(${"$"}chatId: Int!, ${"$"}newAdminId: ID) {
        leaveGroupChat(chatId: ${"$"}chatId, newAdminId: ${"$"}newAdminId)
    }
"""

fun errLeaveGroupChat(accessToken: String, chatId: Int, newAdminId: String? = null): String =
    operateLeaveGroupChat(accessToken, chatId, newAdminId).errors!![0].message

private fun operateLeaveGroupChat(accessToken: String, chatId: Int, newAdminId: String? = null): GraphQlResponse =
    operateQueryOrMutation(
        LEAVE_GROUP_CHAT_QUERY,
        variables = mapOf("chatId" to chatId, "newAdminId" to newAdminId),
        accessToken = accessToken
    )

fun leaveGroupChat(accessToken: String, chatId: Int, newAdminId: String? = null): Boolean =
    operateLeaveGroupChat(accessToken, chatId, newAdminId).data!!["leaveGroupChat"] as Boolean

class LeaveGroupChatTest : FunSpec({
    listener(AppListener())

    test("A non-admin should leave the chat") {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        leaveGroupChat(user.accessToken, chatId)
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(admin.info.id)
    }

    test("The admin should leave the chat after specifying the new admin if there are users left in the chat") {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        leaveGroupChat(admin.accessToken, chatId, newAdminId = user.info.id)
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(user.info.id)
    }

    test("The admin should leave the chat without specifying a new admin if they are the only user") {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        leaveGroupChat(token, chatId)
    }

    fun testBadUserId(supplyingId: Boolean) {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        val newAdminId = if (supplyingId) "invalid new admin ID" else null
        val exception = if (supplyingId) InvalidNewAdminIdException() else MissingNewAdminIdException()
        errLeaveGroupChat(admin.accessToken, chatId, newAdminId) shouldBe exception.message
    }

    test("The admin shouldn't be allowed to leave without specifying a new admin if there are users left") {
        testBadUserId(supplyingId = false)
    }

    test("The admin shouldn't be allowed to leave the chat if the new admin's user ID is invalid") {
        testBadUserId(supplyingId = true)
    }

    test("Leaving a group chat the user is not in should throw an exception") {
        val token = createVerifiedUsers(1)[0].accessToken
        errLeaveGroupChat(token, chatId = 1) shouldBe InvalidChatIdException().message
    }

    test("The user should be unsubscribed from the chat's message updates when they leave the chat") {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        operateMessageUpdates(chatId, user.accessToken) { incoming, _ ->
            leaveGroupChat(user.accessToken, chatId)
            incoming.receive().frameType shouldBe FrameType.CLOSE
        }
    }
})