package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.GroupChatInviteMessages
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import java.util.*

class GroupChatInviteMessage(override val id: Int) : Message, ReadMessageResult {
    fun getInviteCode(): UUID? {
        val chatId = GroupChatInviteMessages.read(id)
        return GroupChats.readInviteCode(chatId)
    }
}
