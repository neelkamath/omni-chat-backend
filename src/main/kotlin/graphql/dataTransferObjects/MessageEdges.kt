@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

/** The [messageIdList] correspond to the edges which will be fetched. They must be sorted in ascending order. */
class MessageEdges(private val messageIdList: LinkedHashSet<Int>) : SearchChatMessagesResult {
    fun getEdges(): List<MessageEdge> = messageIdList.map(::MessageEdge)
}
