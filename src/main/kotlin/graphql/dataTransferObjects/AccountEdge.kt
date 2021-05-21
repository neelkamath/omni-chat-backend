@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class AccountEdge(private val userId: Int) {
    fun getCursor(): Cursor = userId

    fun getNode(): Account = Account(userId)
}
