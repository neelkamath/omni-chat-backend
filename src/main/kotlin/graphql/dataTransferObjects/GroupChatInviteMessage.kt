package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import java.util.*

class GroupChatInviteMessage(override val id: Int) : Message, ReadMessageResult {
    fun getInviteCode(): UUID? {
        val chatId = Messages.readChatId(id)
        return GroupChats.readInviteCode(chatId)
    }
}
