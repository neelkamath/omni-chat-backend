@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ChatEdges
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class ChatMessagesEdge(private val chatEdges: ChatEdges) {
    fun getNode(): ChatMessages = ChatMessages(chatEdges)

    fun getCursor(): Cursor = chatEdges.chatId
}
