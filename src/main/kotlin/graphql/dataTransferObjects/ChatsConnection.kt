@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

/**
 * The [startCursor] and [endCursor] are the IDs of the first and last chats the user is in respectively when the chat
 * IDs are sorted in ascending order.
 * @param chatIdList The chat IDs used to retrieve this connection's edges. They must be sorted in ascending order.
 * @param pagination The pagination used to retrieve the [chatIdList].
 */
class ChatsConnection(
    private val chatIdList: LinkedHashSet<Int>,
    private val startCursor: Cursor?,
    private val endCursor: Cursor?,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<ChatEdge> = chatIdList.map(::ChatEdge)

    fun getPageInfo(): PageInfo = PageInfo(
        startCursor,
        endCursor,
        firstEdgeCursor = chatIdList.firstOrNull(),
        lastEdgeCursor = chatIdList.lastOrNull(),
        pagination,
    )
}
