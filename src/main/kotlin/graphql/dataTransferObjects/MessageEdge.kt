@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class MessageEdge(private val messageId: Int) {
    fun getNode(): Message = Message.build(messageId)

    fun getCursor(): Cursor = messageId
}
