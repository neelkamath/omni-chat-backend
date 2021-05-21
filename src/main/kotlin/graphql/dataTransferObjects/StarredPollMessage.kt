package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class StarredPollMessage(override val id: Int) : StarredMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }

    fun getPoll(): Poll = Poll(id)
}
