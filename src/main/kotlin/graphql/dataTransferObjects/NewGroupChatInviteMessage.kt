package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.GroupChatInviteMessages
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import java.util.*

class NewGroupChatInviteMessage(override val id: Int) : MessagesSubscription, NewMessage, ChatMessagesSubscription {
    fun getInviteCode(): UUID? {
        val chatId = GroupChatInviteMessages.read(id)
        return GroupChats.readInviteCode(chatId)
    }
}
