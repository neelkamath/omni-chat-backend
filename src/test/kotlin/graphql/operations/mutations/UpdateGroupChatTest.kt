package com.neelkamath.omniChat.graphql.operations.mutations

import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.GroupChatUpdate
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.chats.GroupChats
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidNewAdminIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.graphql.operations.requestGraphQlQueryOrMutation
import com.neelkamath.omniChat.shouldHaveUnauthorizedStatus
import graphql.operations.mutations.createGroupChat
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

const val UPDATE_GROUP_CHAT_QUERY = """
    mutation UpdateGroupChat(${"$"}update: GroupChatUpdate!) {
        updateGroupChat(update: ${"$"}update)
    }
"""

private fun operateUpdateGroupChat(accessToken: String, update: GroupChatUpdate): GraphQlResponse =
    operateGraphQlQueryOrMutation(
        UPDATE_GROUP_CHAT_QUERY,
        variables = mapOf("update" to update),
        accessToken = accessToken
    )

fun updateGroupChat(accessToken: String, update: GroupChatUpdate) =
    operateUpdateGroupChat(accessToken, update).data!!["updateGroupChat"]

class UpdateGroupChatTest : FunSpec({
    test("Only the supplied fields should be updated") {
        val (admin, user1, user2) = createSignedInUsers(3)
        val initialUserIdList = listOf(user1.info.id)
        val chat = NewGroupChat("Title", "description", initialUserIdList)
        val chatId = createGroupChat(admin.accessToken, chat)
        val update = GroupChatUpdate(
            chatId,
            "New Title",
            newUserIdList = listOf(user2.info.id),
            removedUserIdList = listOf(user1.info.id)
        )
        updateGroupChat(admin.accessToken, update)
        with(GroupChats.readUserChats(admin.info.id)[0]) {
            adminId shouldBe admin.info.id
            users.edges.map { it.node.id } shouldBe
                    initialUserIdList + admin.info.id + update.newUserIdList - update.removedUserIdList
            title shouldBe update.title
            description shouldBe chat.description
        }
    }

    test("The chat's new admin should be set") {
        val (firstAdmin, secondAdmin) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(secondAdmin.info.id))
        val chatId = createGroupChat(firstAdmin.accessToken, chat)
        val update = GroupChatUpdate(chatId, newAdminId = secondAdmin.info.id)
        updateGroupChat(firstAdmin.accessToken, update)
        GroupChats.readChat(chatId).adminId shouldBe secondAdmin.info.id
    }

    test("Transferring admin status to a user not in the chat should throw an exception") {
        val (admin, notInvitedUser) = createSignedInUsers(2)
        val chatId = createGroupChat(admin.accessToken, NewGroupChat("Title"))
        val update = GroupChatUpdate(chatId, newAdminId = notInvitedUser.info.id)
        val response = operateUpdateGroupChat(admin.accessToken, update)
        response.errors!![0].message shouldBe InvalidNewAdminIdException.message
    }

    test("Updating a nonexistent chat should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        val response = operateUpdateGroupChat(token, GroupChatUpdate(chatId = 1))
        response.errors!![0].message shouldBe InvalidChatIdException.message
    }

    test("Updating a chat the user isn't the admin of should return an authorization error") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat("Title", userIdList = listOf(user.info.id))
        val chatId = createGroupChat(admin.accessToken, chat)
        val variables = mapOf("update" to GroupChatUpdate(chatId))
        requestGraphQlQueryOrMutation(UPDATE_GROUP_CHAT_QUERY, variables, user.accessToken)
            .shouldHaveUnauthorizedStatus()
    }
})