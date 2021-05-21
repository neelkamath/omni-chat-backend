@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.MessageType
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class StarredMessageEdge(private val messageId: Int) {
    fun getCursor(): Cursor = messageId

    fun getNode(): StarredMessage = when (Messages.readType(messageId)) {
        MessageType.VIDEO -> StarredVideoMessage(messageId)
        MessageType.POLL -> StarredPollMessage(messageId)
        MessageType.PIC -> StarredPicMessage(messageId)
        MessageType.GROUP_CHAT_INVITE -> StarredGroupChatInviteMessage(messageId)
        MessageType.DOC -> StarredDocMessage(messageId)
        MessageType.AUDIO -> StarredAudioMessage(messageId)
        MessageType.ACTION -> StarredActionMessage(messageId)
        MessageType.TEXT -> StarredTextMessage(messageId)
    }
}
