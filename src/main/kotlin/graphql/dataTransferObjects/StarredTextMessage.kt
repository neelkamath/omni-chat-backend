@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.TextMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class StarredTextMessage(override val id: Int) : StarredMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }

    fun getTextMessage(): MessageText = TextMessages.read(id)
}
