@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class MessageDateTimeStatusEdge(private val statusId: Int) {
    fun getNode(): MessageDateTimeStatus = MessageDateTimeStatus(statusId)

    fun getCursor(): Cursor = statusId
}
