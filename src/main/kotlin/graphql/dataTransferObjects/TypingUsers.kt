@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.TypingStatuses

/** The users who are typing in the [chatId]. */
class TypingUsers(private val chatId: Int) {
    fun getChatId(): Int = chatId

    fun getUsers(): List<Account> = TypingStatuses.readChat(chatId).map(::Account)
}
