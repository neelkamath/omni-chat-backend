@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.ImageMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class ImageMessage(override val id: Int) : Message, ReadMessageResult {
    fun getCaption(): MessageText? = ImageMessages.readCaption(id)
}
