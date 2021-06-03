package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedProfilePic(private val userId: Int) : AccountsSubscription, ChatAccountsSubscription {
    fun getUserId(): Int = userId
}
