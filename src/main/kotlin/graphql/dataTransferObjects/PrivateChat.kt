@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.PrivateChats
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

class PrivateChat(override val id: Int) : Chat, ReadChatResult {
    fun getUser(env: DataFetchingEnvironment): Account =
        Account(PrivateChats.readOtherUserId(id, env.userId!!))
}
