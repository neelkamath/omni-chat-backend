package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class ChatEdge(private val chatId: Int) {
    fun getNode(): Chat = if (GroupChats.isExisting(chatId)) GroupChat(chatId) else PrivateChat(chatId)

    fun getCursor(): Cursor = chatId
}
