@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.MessageStatuses

class MessageDateTimeStatusConnection(private val messageId: Int, private val pagination: ForwardPagination) {
    private val messageIdList: LinkedHashSet<Int> by lazy { MessageStatuses.readIdList(messageId) }
    private val edgeIdList: List<Int> by lazy {
        messageIdList
            .dropWhile { if (pagination.after == null) false else it <= pagination.after }
            .let { if (pagination.first == null) it else it.take(pagination.first) }
    }

    fun getEdges(): List<MessageDateTimeStatusEdge> = edgeIdList.map(::MessageDateTimeStatusEdge)

    fun getPageInfo(): PageInfo = PageInfo(
        startCursor = messageIdList.firstOrNull(),
        endCursor = messageIdList.lastOrNull(),
        firstEdgeCursor = edgeIdList.firstOrNull(),
        lastEdgeCursor = edgeIdList.lastOrNull(),
        pagination,
    )
}
