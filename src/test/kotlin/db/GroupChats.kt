package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.db.GroupChatUsers
import com.neelkamath.omniChat.db.GroupChats
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Int = Db.transact { selectAll().toList().size }

fun GroupChats.readChat(id: Int): GroupChat = Db.transact {
    val chat = select { GroupChats.id eq id }.first()
    val userIdList = GroupChatUsers.readUserIdList(chat[this.id].value)
    GroupChat(userIdList, chat[title], chat[description])
}