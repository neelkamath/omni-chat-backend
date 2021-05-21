package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class MessageContext(private val messageId: Int) {
    fun getHasContext(): Boolean = Messages.hasContext(messageId)

    fun getMessageId(): Int? = Messages.readContextMessageId(messageId)
}
