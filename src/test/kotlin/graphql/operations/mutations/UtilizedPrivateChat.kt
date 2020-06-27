package com.neelkamath.omniChat.graphql.operations.mutations

import com.neelkamath.omniChat.graphql.SignedInUser
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.messageAndReadId
import graphql.operations.mutations.createPrivateChat

/** A private chat between two users where [user2] sent the [messageId]. */
data class UtilizedPrivateChat(val messageId: Int, val user1: SignedInUser, val user2: SignedInUser)

fun createUtilizedPrivateChat(): UtilizedPrivateChat {
    val (user1, user2) = createSignedInUsers(2)
    val chatId = createPrivateChat(user1.accessToken, user2.info.id)
    val messageId = messageAndReadId(user2.accessToken, chatId, "text")
    return UtilizedPrivateChat(messageId, user1, user2)
}