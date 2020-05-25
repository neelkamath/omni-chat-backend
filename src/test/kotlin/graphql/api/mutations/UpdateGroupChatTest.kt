package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidNewAdminIdException
import com.neelkamath.omniChat.graphql.UnauthorizedException
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.operateQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val UPDATE_GROUP_CHAT_QUERY: String = """
    mutation UpdateGroupChat(${"$"}update: GroupChatUpdate!) {
        updateGroupChat(update: ${"$"}update)
    }
"""

private fun operateUpdateGroupChat(update: GroupChatUpdate, accessToken: String): GraphQlResponse =
    operateQueryOrMutation(UPDATE_GROUP_CHAT_QUERY, variables = mapOf("update" to update), accessToken = accessToken)

fun updateGroupChat(update: GroupChatUpdate, accessToken: String): Boolean =
    operateUpdateGroupChat(update, accessToken).data!!["updateGroupChat"] as Boolean

class UpdateGroupChatTest : FunSpec({
    test("Only the supplied fields should be updated") {
        val (admin, user1, user2) = createVerifiedUsers(3)
        val initialUserIdList = setOf(user1.info.id)
        val chat = NewGroupChat("Title", "description", initialUserIdList)
        val chatId = createGroupChat(chat, admin.accessToken)
        val update = GroupChatUpdate(
            chatId,
            "New Title",
            newUserIdList = setOf(user2.info.id),
            removedUserIdList = setOf(user1.info.id)
        )
        updateGroupChat(update, admin.accessToken)
        val userIdList = initialUserIdList + admin.info.id + update.newUserIdList - update.removedUserIdList
        val users = userIdList.map(::findUserById).toSet()
        GroupChats.read(admin.info.id) shouldBe listOf(
            GroupChat(chatId, admin.info.id, users, update.title!!, chat.description, Messages.readChat(chatId))
        )
    }

    test("The chat's new admin should be set") {
        val (firstAdmin, secondAdmin) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(secondAdmin.info.id))
        val chatId = createGroupChat(chat, firstAdmin.accessToken)
        val update = GroupChatUpdate(chatId, newAdminId = secondAdmin.info.id)
        updateGroupChat(update, firstAdmin.accessToken)
        GroupChats.read(chatId).adminId shouldBe secondAdmin.info.id
    }

    test("Transferring admin status to a user not in the chat should throw an exception") {
        val (admin, notInvitedUser) = createVerifiedUsers(2)
        val chatId = createGroupChat(NewGroupChat("Title"), admin.accessToken)
        val update = GroupChatUpdate(chatId, newAdminId = notInvitedUser.info.id)
        val response = operateUpdateGroupChat(update, admin.accessToken)
        response.errors!![0].message shouldBe InvalidNewAdminIdException.message
    }

    test("Updating a nonexistent chat should throw an exception") {
        val token = createVerifiedUsers(1)[0].accessToken
        val response = operateUpdateGroupChat(GroupChatUpdate(chatId = 1), token)
        response.errors!![0].message shouldBe InvalidChatIdException.message
    }

    test("Updating a chat the user isn't the admin of should throw an exception") {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, admin.accessToken)
        val response = operateUpdateGroupChat(GroupChatUpdate(chatId), user.accessToken)
        response.errors!![0].message shouldBe UnauthorizedException.message
    }
})