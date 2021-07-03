@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages
import java.time.LocalDateTime

sealed interface BookmarkedMessage {
    /** The [Messages.id]. */
    val id: Int

    fun getMessageId(): Int = id

    fun getSender(): Account = Account(Messages.readSenderId(id))

    fun getSent(): LocalDateTime = Messages.readSent(id)

    fun getContext(): MessageContext = MessageContext(id)

    fun getIsForwarded(): Boolean = Messages.isForwarded(id)

    /** The ID of the chat which this message this belongs to. */
    val chatId: Lazy<Int>

    fun getChatId(): Int = chatId.value
}
