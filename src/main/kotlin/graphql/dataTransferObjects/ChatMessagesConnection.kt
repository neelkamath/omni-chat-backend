@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ChatEdges
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class ChatMessagesConnection(
    private val startCursor: Cursor?,
    private val endCursor: Cursor?,
    private val chatEdgesList: LinkedHashSet<ChatEdges>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<ChatMessagesEdge> = chatEdgesList.map(::ChatMessagesEdge)

    fun getPageInfo(): PageInfo = PageInfo(
        startCursor,
        endCursor,
        firstEdgeCursor = chatEdgesList.firstOrNull()?.chatId,
        lastEdgeCursor = chatEdgesList.lastOrNull()?.chatId,
        pagination,
    )
}
