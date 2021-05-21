@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.Stargazers
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

/**
 * @param messageIdList The message IDs (sorted in ascending order) used to retrieve the edges for this connection.
 * @param pagination The pagination used to retrieve the [messageIdList].
 */
class StarredMessagesConnection(
    private val messageIdList: LinkedHashSet<Int>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<StarredMessageEdge> = messageIdList.map(::StarredMessageEdge)

    fun getPageInfo(env: DataFetchingEnvironment): PageInfo = PageInfo(
        Stargazers.readCursor(env.userId!!, CursorType.START),
        Stargazers.readCursor(env.userId!!, CursorType.END),
        firstEdgeCursor = messageIdList.firstOrNull(),
        lastEdgeCursor = messageIdList.lastOrNull(),
        pagination,
    )
}
