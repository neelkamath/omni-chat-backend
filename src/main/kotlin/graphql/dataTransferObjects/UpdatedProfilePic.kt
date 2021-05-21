package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class UpdatedProfilePic(private val userId: Int) : AccountsSubscription {
    fun getUserId(): Int = userId
}
