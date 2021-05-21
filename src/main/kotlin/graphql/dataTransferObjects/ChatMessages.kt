@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.ChatEdges
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import graphql.schema.DataFetchingEnvironment

class ChatMessages(private val chatEdges: ChatEdges) {
    fun getChat(): Chat =
        if (GroupChats.isExisting(chatEdges.chatId)) GroupChat(chatEdges.chatId)
        else PrivateChat(chatEdges.chatId)

    fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return chatEdges
            .messageIdList
            .let { list ->
                if (pagination.before == null) list else list.takeWhile { it < pagination.before }
            }
            .let { if (pagination.last == null) it else it.toList().takeLast(pagination.last) }
            .map(::MessageEdge)
    }
}
