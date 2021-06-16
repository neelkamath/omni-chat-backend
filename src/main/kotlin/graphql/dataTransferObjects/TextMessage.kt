@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.TextMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class TextMessage(override val id: Int) : Message, ReadMessageResult {
    fun getTextMessage(): MessageText = TextMessages.read(id)
}
