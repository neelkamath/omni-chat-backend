package com.neelkamath.omniChat.graphql.operations.mutations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.InvalidGroupChatUsersException
import com.neelkamath.omniChat.graphql.InvalidNewAdminIdException
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.operateGraphQlQueryOrMutation
import com.neelkamath.omniChat.graphql.operations.requestGraphQlQueryOrMutation
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

fun updateGroupChat(accessToken: String, update: GroupChatUpdate): Placeholder {
    val data = operateUpdateGroupChat(accessToken, update).data!!["updateGroupChat"] as String
    return objectMapper.convertValue(data)
}

fun errUpdateGroupChat(accessToken: String, update: GroupChatUpdate): String =
    operateUpdateGroupChat(accessToken, update).errors!![0].message

class UpdateGroupChatTest : FunSpec({
    test("Only the supplied fields should be updated") {
        val (admin, user1, user2) = createSignedInUsers(3)
        val initialUserIdList = listOf(user1.info.id)
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription("description"), initialUserIdList)
        val chatId = createGroupChat(admin.accessToken, chat)
        val update = GroupChatUpdate(
            chatId,
            GroupChatTitle("New Title"),
            newUserIdList = listOf(user2.info.id),
            removedUserIdList = listOf(user1.info.id)
        )
        updateGroupChat(admin.accessToken, update)
        with(GroupChats.readUserChats(admin.info.id)[0]) {
            adminId shouldBe admin.info.id
            users.edges.map { it.node.id } shouldBe
                    initialUserIdList + admin.info.id + update.newUserIdList!! - update.removedUserIdList!!
            title shouldBe update.title
            description shouldBe chat.description
        }
    }

    test("The chat's new admin should be set") {
        val (firstAdmin, secondAdmin) = createSignedInUsers(2)
        val chat = NewGroupChat(
            GroupChatTitle("Title"),
            GroupChatDescription("description"),
            userIdList = listOf(secondAdmin.info.id)
        )
        val chatId = createGroupChat(firstAdmin.accessToken, chat)
        val update = GroupChatUpdate(chatId, newAdminId = secondAdmin.info.id)
        updateGroupChat(firstAdmin.accessToken, update)
        GroupChats.readChat(chatId).adminId shouldBe secondAdmin.info.id
    }

    test("Updating a nonexistent chat should throw an exception") {
        val token = createSignedInUsers(1)[0].accessToken
        errUpdateGroupChat(token, GroupChatUpdate(chatId = 1)) shouldBe InvalidChatIdException.message
    }

    test("Updating a chat the user isn't the admin of should return an authorization error") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat(
            GroupChatTitle("Title"),
            GroupChatDescription("description"),
            userIdList = listOf(user.info.id)
        )
        val chatId = createGroupChat(admin.accessToken, chat)
        val variables = mapOf("update" to GroupChatUpdate(chatId))
        requestGraphQlQueryOrMutation(UPDATE_GROUP_CHAT_QUERY, variables, user.accessToken)
            .shouldHaveUnauthorizedStatus()
    }

    test("Transferring admin status to a user not in the chat should throw an exception") {
        val (admin, notInvitedUser) = createSignedInUsers(2)
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chatId = createGroupChat(admin.accessToken, chat)
        val update = GroupChatUpdate(chatId, newAdminId = notInvitedUser.info.id)
        errUpdateGroupChat(admin.accessToken, update) shouldBe InvalidNewAdminIdException.message
    }

    test("Adding and removing the same user at the same time should fail") {
        val (admin, user) = createSignedInUsers(2)
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chatId = createGroupChat(admin.accessToken, chat)
        val update = mapOf(
            "update" to mapOf(
                "chatId" to chatId,
                "newUserIdList" to listOf(user.info.id),
                "removedUserIdList" to listOf(user.info.id)
            )
        )
        operateGraphQlQueryOrMutation(UPDATE_GROUP_CHAT_QUERY, variables = update, accessToken = admin.accessToken)
            .errors!![0]
            .message shouldBe InvalidGroupChatUsersException.message
    }
})