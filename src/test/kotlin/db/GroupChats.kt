package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.db.DB
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.read(): List<GroupChat> = DB.transact {
    GroupChats.selectAll().map {
        val userIdList = GroupChatUsers.read(it[id].value)
        GroupChat(userIdList, it[title], it[description])
    }
}