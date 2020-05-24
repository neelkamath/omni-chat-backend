package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GroupChatUsersTest : FunSpec({
    context("addUsers(Int, Set<String>)") {
        test("Users should be added to the chat, ignoring the ones already in it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val initialUserIdList = setOf(user1Id)
            val chat = NewGroupChat("Title", userIdList = initialUserIdList)
            val chatId = GroupChats.create(adminId, chat)
            val newUserIdList = setOf(user2Id)
            GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
            GroupChatUsers.readUserIdList(chatId) shouldBe initialUserIdList + newUserIdList + adminId
        }
    }

    context("removeUsers(Int, Set<String>)") {
        test("A user who leaves the chat should have their subscription removed") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val subscriber = createMessageUpdatesSubscriber(userId, chatId)
            GroupChatUsers.removeUsers(chatId, setOf(userId))
            subscriber.assertComplete()
        }
    }
})