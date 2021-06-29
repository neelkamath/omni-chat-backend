@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.Users

class OnlineStatus(private val userId: Int) :
    ReadOnlineStatusResult,
    OnlineStatusesSubscription,
    ChatOnlineStatusesSubscription {

    fun getUserId(): Int = userId

    fun getIsOnline(): Boolean = Users.isOnline(userId)
}
