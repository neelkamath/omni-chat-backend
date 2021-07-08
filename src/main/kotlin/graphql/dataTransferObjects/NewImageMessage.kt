@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.ImageMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class NewImageMessage(override val id: Int) : MessagesSubscription, NewMessage, ChatMessagesSubscription {
    fun getCaption(): MessageText? = ImageMessages.readCaption(id)
}
