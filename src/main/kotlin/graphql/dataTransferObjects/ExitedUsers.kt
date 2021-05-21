@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class ExitedUsers(private val chatId: Int, private val userIdList: List<Int>) : GroupChatsSubscription {
    fun getChatId(): Int = chatId

    fun getUserIdList(): List<Int> = userIdList
}
