@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.MessageStatuses
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.graphql.routing.MessageState
import java.time.LocalDateTime

interface StarredMessage {
    /** The [Messages.id]. */
    val id: Int

    /** The ID of the chat which this message this belongs to. */
    val chatId: Lazy<Int>

    fun getChatId(): Int = chatId.value

    fun getMessageId(): Int = id

    fun getSender(): Account = Account(Messages.readSenderId(id))

    fun getState(): MessageState = Messages.readState(id)

    fun getSent(): LocalDateTime = Messages.readSent(id)

    fun getStatuses(): List<MessageDateTimeStatus> =
        MessageStatuses.readIdList(id).map(::MessageDateTimeStatus)

    fun getContext(): MessageContext = MessageContext(id)

    fun getIsForwarded(): Boolean = Messages.isForwarded(id)
}
