package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.GroupChatInviteMessages
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import java.util.*

class StarredGroupChatInviteMessage(override val id: Int) : StarredMessage {
    override val chatId: Lazy<Int> = lazy { Messages.readChatId(id) }

    fun getInviteCode(): UUID? {
        val chatId = GroupChatInviteMessages.read(id)
        return GroupChats.readInviteCode(chatId)
    }
}
