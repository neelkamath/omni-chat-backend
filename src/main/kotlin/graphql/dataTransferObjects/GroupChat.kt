package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.GroupChats
import java.util.*

class GroupChat(override val id: Int) : BareGroupChat, Chat, ReadChatResult {
    fun getInviteCode(): UUID? = GroupChats.readInviteCode(id)
}
