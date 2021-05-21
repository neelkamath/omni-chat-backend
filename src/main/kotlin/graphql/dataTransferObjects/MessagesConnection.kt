@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

/** The [messageIdList] are the message IDs (sorted in ascending order) which correspond to this connection's edges. */
class MessagesConnection(
    private val chatId: Int,
    private val messageIdList: LinkedHashSet<Int>,
    private val pagination: BackwardPagination? = null,
) {
    fun getEdges(): List<MessageEdge> = messageIdList.map(::MessageEdge)

    fun getPageInfo(env: DataFetchingEnvironment): PageInfo {
        val isGroupChat = GroupChats.isExisting(chatId)
        val startCursor =
            if (isGroupChat) Messages.readGroupChatCursor(chatId, CursorType.START)
            else Messages.readPrivateChatCursor(env.userId!!, chatId, CursorType.START)
        val endCursor =
            if (isGroupChat) Messages.readGroupChatCursor(chatId, CursorType.END)
            else Messages.readPrivateChatCursor(env.userId!!, chatId, CursorType.END)
        return PageInfo(
            startCursor,
            endCursor,
            firstEdgeCursor = messageIdList.firstOrNull(),
            lastEdgeCursor = messageIdList.lastOrNull(),
            pagination,
        )
    }
}
