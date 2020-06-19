package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class GroupChatUsersTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    context("addUsers(Int, Set<String>)") {
        test("Users should be added to the chat, ignoring the ones already in it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val initialUserIdList = listOf(user1Id)
            val chat = NewGroupChat("Title", userIdList = initialUserIdList)
            val chatId = GroupChats.create(adminId, chat)
            val newUserIdList = listOf(user2Id)
            GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
            GroupChatUsers.readUserIdList(chatId) shouldContainExactlyInAnyOrder
                    initialUserIdList + newUserIdList + adminId
        }
    }

    context("removeUsers(Int, List<String>)") {
        test("A user who leaves the chat should have their subscription removed") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val subscriber = createMessageUpdatesSubscriber(userId, chatId)
            GroupChatUsers.removeUsers(chatId, listOf(userId))
            subscriber.assertComplete()
        }
    }
}