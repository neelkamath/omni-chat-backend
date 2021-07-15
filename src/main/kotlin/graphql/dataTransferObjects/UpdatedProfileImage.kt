package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedProfileImage(private val userId: Int) : AccountsSubscription, ChatAccountsSubscription {
    fun getUserId(): Int = userId
}
