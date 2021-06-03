@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.graphql.routing.MessageState
import graphql.schema.DataFetchingEnvironment
import java.time.LocalDateTime

sealed interface StarredMessage {
    /** The [Messages.id]. */
    val id: Int

    fun getMessageId(): Int = id

    fun getSender(): Account = Account(Messages.readSenderId(id))

    fun getState(): MessageState = Messages.readState(id)

    fun getSent(): LocalDateTime = Messages.readSent(id)

    fun getStatuses(env: DataFetchingEnvironment): MessageDateTimeStatusConnection {
        val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
        return MessageDateTimeStatusConnection(id, pagination)
    }

    fun getContext(): MessageContext = MessageContext(id)

    fun getIsForwarded(): Boolean = Messages.isForwarded(id)

    /** The ID of the chat which this message this belongs to. */
    val chatId: Lazy<Int>

    fun getChatId(): Int = chatId.value
}
