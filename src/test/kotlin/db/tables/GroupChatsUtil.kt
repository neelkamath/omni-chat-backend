package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.graphql.routing.GroupChatDescription
import com.neelkamath.omniChat.graphql.routing.GroupChatInput
import com.neelkamath.omniChat.graphql.routing.GroupChatTitle

/**
 * Returns the created chat's ID. It doesn't matter whether [adminIdList] and [userIdList] intersect. If [isPublic],
 * [isInvitable] will be set to `true` regardless of what you set it to.
 */
fun GroupChats.create(
    adminIdList: List<Int>,
    userIdList: List<Int> = listOf(),
    title: GroupChatTitle = GroupChatTitle("T"),
    description: GroupChatDescription = GroupChatDescription(""),
    isBroadcast: Boolean = false,
    isPublic: Boolean = false,
    isInvitable: Boolean = false
): Int = create(
    GroupChatInput(
        title,
        description,
        userIdList = userIdList + adminIdList,
        adminIdList = adminIdList,
        isBroadcast = isBroadcast,
        isPublic = isPublic,
        isInvitable = if (isPublic) true else isInvitable
    )
)