@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.PicMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class NewPicMessage(override val id: Int) : MessagesSubscription, NewMessage, ChatMessagesSubscription {
    fun getCaption(): MessageText? = PicMessages.readCaption(id)
}
