@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.PicMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class StarredPicMessage(override val id: Int) : StarredMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }

    fun getCaption(): MessageText? = PicMessages.readCaption(id)
}
