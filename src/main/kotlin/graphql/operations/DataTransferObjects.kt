@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.operations

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.Stargazers
import com.neelkamath.omniChatBackend.graphql.routing.PageInfo
import com.neelkamath.omniChatBackend.graphql.routing.StarredMessage
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

/**
 * @param messageIdList The message IDs used to retrieve the edges for this connection.
 * @param pagination The pagination used to retrieve the [messageIdList].
 */
class StarredMessagesConnectionDto(
    private val messageIdList: LinkedHashSet<Int>,
    private val pagination: ForwardPagination? = null,
) {
    fun getEdges(): List<StarredMessageEdgeDto> = messageIdList.map(::StarredMessageEdgeDto)

    fun getPageInfo(env: DataFetchingEnvironment): PageInfo =
        Stargazers.readPageInfo(env.userId!!, messageIdList.lastOrNull(), pagination)
}

class StarredMessageEdgeDto(private val messageId: Int) {
    fun getCursor(): Int = messageId

    fun getNode(env: DataFetchingEnvironment): StarredMessage = StarredMessage.build(env.userId!!, messageId)
}
