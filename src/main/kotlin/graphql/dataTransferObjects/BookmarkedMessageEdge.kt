@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.MessageType
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.graphql.routing.Cursor

class BookmarkedMessageEdge(private val messageId: Int) {
    fun getCursor(): Cursor = messageId

    fun getNode(): BookmarkedMessage = when (Messages.readType(messageId)) {
        MessageType.VIDEO -> BookmarkedVideoMessage(messageId)
        MessageType.POLL -> BookmarkedPollMessage(messageId)
        MessageType.IMAGE -> BookmarkedImageMessage(messageId)
        MessageType.GROUP_CHAT_INVITE -> BookmarkedGroupChatInviteMessage(messageId)
        MessageType.DOC -> BookmarkedDocMessage(messageId)
        MessageType.AUDIO -> BookmarkedAudioMessage(messageId)
        MessageType.ACTION -> BookmarkedActionMessage(messageId)
        MessageType.TEXT -> BookmarkedTextMessage(messageId)
    }
}
