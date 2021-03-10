package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.graphql.routing.GroupChatDescription
import com.neelkamath.omniChat.graphql.routing.GroupChatInput
import com.neelkamath.omniChat.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChat.graphql.routing.GroupChatTitle

/** Returns the created chat's ID. It doesn't matter whether [adminIdList] and [userIdList] intersect. */
fun GroupChats.create(
    adminIdList: Collection<Int>,
    userIdList: Collection<Int> = listOf(),
    title: GroupChatTitle = GroupChatTitle("T"),
    description: GroupChatDescription = GroupChatDescription(""),
    isBroadcast: Boolean = false,
    publicity: GroupChatPublicity = GroupChatPublicity.NOT_INVITABLE,
): Int = create(
    GroupChatInput(
        title,
        description,
        userIdList = userIdList + adminIdList,
        adminIdList = adminIdList.toList(),
        isBroadcast = isBroadcast,
        publicity = publicity,
    )
)
