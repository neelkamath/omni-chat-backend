@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Bookmarks
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment

class UpdatedMessage(private val messageId: Int) : MessagesSubscription, ChatMessagesSubscription {
    fun getChatId(): Int = Messages.readChatId(messageId)

    fun getMessageId(): Int = messageId

    fun getIsBookmarked(env: DataFetchingEnvironment): Boolean = Bookmarks.isBookmarked(env.userId!!, messageId)
}
