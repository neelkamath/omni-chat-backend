package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class BookmarkedPollMessage(override val id: Int) : BookmarkedMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }

    fun getPoll(): Poll = Poll(id)
}
