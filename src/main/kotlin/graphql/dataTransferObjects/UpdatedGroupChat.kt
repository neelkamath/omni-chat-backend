package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.GroupChatDescription
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatTitle

class UpdatedGroupChat(
    private val chatId: Int,
    private val title: GroupChatTitle? = null,
    private val description: GroupChatDescription? = null,
    private val newUserIdList: List<Int>? = null,
    private val removedUserIdList: List<Int>? = null,
    private val adminIdList: List<Int>? = null,
    private val isBroadcast: Boolean? = null,
    private val publicity: GroupChatPublicity? = null,
) : GroupChatsSubscription {
    fun getChatId(): Int = chatId

    fun getTitle(): GroupChatTitle? = title

    fun getDescription(): GroupChatDescription? = description

    fun getNewUsers(): List<Account>? = newUserIdList?.map(::Account)

    fun getRemovedUsers(): List<Account>? = removedUserIdList?.map(::Account)

    fun getAdminIdList(): List<Int>? = adminIdList

    fun getIsBroadcast(): Boolean? = isBroadcast

    fun getPublicity(): GroupChatPublicity? = publicity
}
