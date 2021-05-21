@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class GroupChatEdge(private val chatId: Int) {
    fun getNode(): GroupChat = GroupChat(chatId)

    fun getCursor(): Cursor = chatId
}
