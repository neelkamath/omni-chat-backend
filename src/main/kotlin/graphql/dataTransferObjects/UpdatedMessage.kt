@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.Stargazers
import com.neelkamath.omniChatBackend.graphql.routing.MessageState
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

class UpdatedMessage(private val messageId: Int) : MessagesSubscription, ChatMessagesSubscription {
    fun getChatId(): Int = Messages.readChatId(messageId)

    fun getMessageId(): Int = messageId

    fun getState(): MessageState = Messages.readState(messageId)

    fun getStatuses(env: DataFetchingEnvironment): MessageDateTimeStatusConnection {
        val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("last"))
        return MessageDateTimeStatusConnection(messageId, pagination)
    }

    fun getHasStar(env: DataFetchingEnvironment): Boolean = Stargazers.hasStar(env.userId!!, messageId)
}
