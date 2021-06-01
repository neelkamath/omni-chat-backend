package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class DeletedAccount(private val userId: Int) : AccountsSubscription, ChatAccountsSubscription {
    fun getUserId(): Int = userId
}
