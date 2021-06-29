@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages
import java.time.LocalDateTime

sealed interface NewMessage {
    /** The [Messages.id]. */
    val id: Int

    fun getChatId(): Int = Messages.readChatId(id)

    fun getMessageId(): Int = id

    fun getSender(): Account = Account(Messages.readSenderId(id))

    fun getSent(): LocalDateTime = Messages.readSent(id)

    fun getContext(): MessageContext = MessageContext(id)

    fun getIsForwarded(): Boolean = Messages.isForwarded(id)
}
