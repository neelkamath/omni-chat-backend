package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class DeletedContact(private val userId: Int) : AccountsSubscription {
    fun getUserId(): Int = userId
}
