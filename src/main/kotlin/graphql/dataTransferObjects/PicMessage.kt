@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.PicMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class PicMessage(override val id: Int) : Message, ReadMessageResult {
    fun getCaption(): MessageText? = PicMessages.readCaption(id)
}
