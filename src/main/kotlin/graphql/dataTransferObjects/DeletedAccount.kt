package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class DeletedAccount(private val userId: Int) : AccountsSubscription {
    fun getUserId(): Int = userId
}
