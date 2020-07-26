package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.GroupChatDescription
import com.neelkamath.omniChat.GroupChatInput
import com.neelkamath.omniChat.GroupChatTitle

/**
 * Returns the created chat's ID. It doesn't matter whether [adminIdList] and [userIdList] intersect. The chat's title
 * and description will be `"T"` and `""` respectively. It won't be a broadcast chat.
 */
fun GroupChats.create(adminIdList: List<Int>, userIdList: List<Int> = listOf()): Int {
    val chat = GroupChatInput(
        GroupChatTitle("t"),
        GroupChatDescription(""),
        userIdList = userIdList + adminIdList,
        adminIdList = adminIdList,
        isBroadcast = false
    )
    return create(chat)
}