package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

fun GroupChats.count(): Long = transact { selectAll().count() }

/** Creates a group chat with the given [adminId] and [userIdList], and returns the chat's ID. */
fun GroupChats.create(adminId: String, userIdList: List<String> = listOf()): Int {
    val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription("description"), userIdList.toList())
    return create(adminId, chat)
}