@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class GroupChatsConnection(
    private val startCursor: Cursor?,
    private val endCursor: Cursor?,
    private val chatIdList: LinkedHashSet<Int>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<GroupChatEdge> = chatIdList.map(::GroupChatEdge)

    fun getPageInfo(): PageInfo = PageInfo(
        startCursor,
        endCursor,
        firstEdgeCursor = chatIdList.firstOrNull(),
        lastEdgeCursor = chatIdList.lastOrNull(),
        pagination,
    )
}
