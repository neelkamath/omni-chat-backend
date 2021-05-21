@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.ActionMessageActions
import com.neelkamath.omniChatBackend.db.tables.ActionMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class ActionableMessage(private val messageId: Int) {
    fun getText(): MessageText = ActionMessages.readText(messageId)

    fun getActions(): List<MessageText> = ActionMessageActions.read(messageId).toList()
}
