package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class BookmarkedDocMessage(override val id: Int) : BookmarkedMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }
}
