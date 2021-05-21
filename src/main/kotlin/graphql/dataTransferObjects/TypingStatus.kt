@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.TypingStatuses

class TypingStatus(private val chatId: Int, private val userId: Int) : TypingStatusesSubscription {
    fun getChatId(): Int = chatId

    fun getUserId(): Int = userId

    fun getIsTyping(): Boolean = TypingStatuses.isTyping(chatId, userId)
}
