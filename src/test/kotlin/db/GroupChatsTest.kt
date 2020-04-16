package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.GroupChatUpdate
import com.neelkamath.omniChat.db.GroupChats
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeZero

class GroupChatsTest : StringSpec({
    listener(DbListener())

    "The chat should be deleted once every user has left it" {
        val adminId = "admin user ID"
        val userIdList = setOf("user 1 ID", "user 2 ID")
        val chatId = GroupChats.create(adminId, GroupChat(userIdList, "Title"))
        GroupChats.update(GroupChatUpdate(chatId, removedUserIdList = userIdList + adminId))
        GroupChats.count().shouldBeZero()
    }
})