package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GroupChatUsersTest : FunSpec({
    listener(DbListener())

    context("addUsers(Int, Set<String>)") {
        test("Users should be added to the chat, ignoring the ones already in it") {
            val admin = "admin user ID"
            val initialUserIdList = setOf("user ID")
            val chat = NewGroupChat("Title", userIdList = initialUserIdList)
            val chatId = GroupChats.create(admin, chat)
            val newUserIdList = setOf("new user")
            GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
            GroupChatUsers.readUserIdList(chatId) shouldBe initialUserIdList + newUserIdList + admin
        }
    }

    context("removeUsers(Int, Set<String>)") {
        test("A user who leaves the chat should have their subscription removed") {
            val (adminId, userId) = (1..2).map { "user $it ID" }
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val subscriber = createMessageUpdatesSubscriber(userId, chatId)
            GroupChatUsers.removeUsers(chatId, setOf(userId))
            subscriber.assertComplete()
        }
    }
})