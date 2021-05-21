package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Messages

class StarredVideoMessage(override val id: Int) : StarredMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }
}
