@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.Bookmarks
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

/**
 * @param messageIdList The message IDs (sorted in ascending order) used to retrieve the edges for this connection.
 * @param pagination The pagination used to retrieve the [messageIdList].
 */
class BookmarkedMessagesConnection(
    private val messageIdList: LinkedHashSet<Int>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<BookmarkedMessageEdge> = messageIdList.map(::BookmarkedMessageEdge)

    fun getPageInfo(env: DataFetchingEnvironment): PageInfo = PageInfo(
        Bookmarks.readCursor(env.userId!!, CursorType.START),
        Bookmarks.readCursor(env.userId!!, CursorType.END),
        firstEdgeCursor = messageIdList.firstOrNull(),
        lastEdgeCursor = messageIdList.lastOrNull(),
        pagination,
    )
}
