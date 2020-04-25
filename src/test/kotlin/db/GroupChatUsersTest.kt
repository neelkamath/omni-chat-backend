package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GroupChatUsersTest : StringSpec({
    listener(DbListener())

    "Users should be added to the chat, ignoring the ones already in it" {
        val admin = "admin user ID"
        val initialUserIdList = setOf("user ID")
        val chatId = GroupChats.create(admin, NewGroupChat("Title", userIdList = initialUserIdList))
        val newUserIdList = setOf("new user")
        GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
        GroupChatUsers.readUserIdList(chatId) shouldBe initialUserIdList + newUserIdList + admin
    }
})