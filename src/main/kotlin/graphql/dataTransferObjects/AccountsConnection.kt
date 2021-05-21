@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

/**
 * @param userIdList The user IDs used to retrieve this connection's edges. They must be sorted in ascending order.
 * @param pagination The pagination used to retrieve the [userIdList].
 */
class AccountsConnection(
    private val startCursor: Cursor?,
    private val endCursor: Cursor?,
    private val userIdList: LinkedHashSet<Int>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<AccountEdge> = userIdList.map(::AccountEdge)

    fun getPageInfo(): PageInfo = PageInfo(
        startCursor,
        endCursor,
        firstEdgeCursor = userIdList.firstOrNull(),
        lastEdgeCursor = userIdList.lastOrNull(),
        pagination,
    )
}
