@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.TextMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class NewTextMessage(override val id: Int) : MessagesSubscription, NewMessage {
    fun getTextMessage(): MessageText = TextMessages.read(id)
}
