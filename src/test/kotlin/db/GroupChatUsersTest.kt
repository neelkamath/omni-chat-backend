package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class GroupChatUsersTest : StringSpec({
    listener(DbListener())

    "Users should be added to the group chat, ignoring the ones already in it" {
        val initialUserIdList = setOf("user ID")
        val chatId = GroupChats.create("admin user ID", GroupChat(initialUserIdList, "Title"))
        val newUserIdList = setOf("new user")
        GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
        GroupChatUsers.readUserIdList(chatId) shouldBe initialUserIdList + newUserIdList
    }
})