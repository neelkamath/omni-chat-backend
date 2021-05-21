@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class StarredActionMessage(override val id: Int) : StarredMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }

    fun getActionableMessage(): ActionableMessage = ActionableMessage(id)
}
