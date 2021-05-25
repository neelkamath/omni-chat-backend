@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.tables.Chats
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.PrivateChats
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

sealed interface Chat {
    /** The [Chats.id]. */
    val id: Int

    fun getChatId(): Int = id

    fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        val messageIdList =
            if (PrivateChats.isExisting(id)) Messages.readPrivateChat(env.userId!!, id, pagination)
            else Messages.readGroupChat(id, pagination)
        return MessagesConnection(id, messageIdList, pagination)
    }
}
