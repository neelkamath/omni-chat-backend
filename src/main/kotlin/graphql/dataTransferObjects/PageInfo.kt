@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.Pagination
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

/**
 * The [firstEdgeCursor] and [lastEdgeCursor] are the first and last [Cursor]s retrieved by the [pagination]
 * respectively.
 */
class PageInfo(
    private val startCursor: Cursor?,
    private val endCursor: Cursor?,
    private val firstEdgeCursor: Cursor?,
    private val lastEdgeCursor: Cursor?,
    private val pagination: Pagination?,
) {
    fun getHasNextPage(): Boolean = when (pagination) {
        null -> false

        is ForwardPagination -> when {
            startCursor == null || endCursor == null -> false
            lastEdgeCursor != null -> lastEdgeCursor < endCursor
            pagination.after == null -> true
            else -> pagination.after < endCursor
        }

        is BackwardPagination -> when {
            startCursor == null || endCursor == null -> false
            lastEdgeCursor != null -> lastEdgeCursor < endCursor
            pagination.before == null -> false
            else -> pagination.before <= endCursor
        }
    }

    fun getHasPreviousPage(): Boolean = when (pagination) {
        null -> false

        is ForwardPagination -> when {
            startCursor == null || endCursor == null -> false
            firstEdgeCursor != null -> startCursor < firstEdgeCursor
            pagination.after == null -> false
            else -> startCursor <= pagination.after
        }

        is BackwardPagination -> when {
            startCursor == null || endCursor == null -> false
            firstEdgeCursor != null -> startCursor < firstEdgeCursor
            pagination.before == null -> true
            else -> startCursor < pagination.before
        }
    }

    fun getStartCursor(): Cursor? = startCursor

    fun getEndCursor(): Cursor? = endCursor
}
