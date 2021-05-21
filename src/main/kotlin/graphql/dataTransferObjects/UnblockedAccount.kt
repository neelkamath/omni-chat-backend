package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UnblockedAccount(private val userId: Int) : AccountsSubscription {
    fun getUserId(): Int = userId
}
