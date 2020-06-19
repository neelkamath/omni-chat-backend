package com.neelkamath.omniChat.test.graphql.api

import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.test.graphql.SignedInUser
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import com.neelkamath.omniChat.test.graphql.api.subscriptions.parseFrameData
import com.neelkamath.omniChat.test.graphql.api.subscriptions.receiveMessageUpdates
import com.neelkamath.omniChat.test.graphql.createSignedInUsers

/** A private chat between two users where [user2] sent the [messageId]. */
data class UtilizedPrivateChat(val messageId: Int, val user1: SignedInUser, val user2: SignedInUser)

fun createUtilizedPrivateChat(): UtilizedPrivateChat {
    val (user1, user2) = createSignedInUsers(2)
    val chatId = createPrivateChat(user1.accessToken, user2.info.id)
    val messageId = messageAndReadId(user2.accessToken, chatId, "text")
    return UtilizedPrivateChat(messageId, user1, user2)
}

/** Has the user who bears the [accessToken] send a [text] in the [chatId], and returns the message's ID. */
fun messageAndReadId(accessToken: String, chatId: Int, text: String): Int {
    var id: Int? = null
    receiveMessageUpdates(accessToken, chatId) { incoming, _ ->
        createMessage(accessToken, chatId, text)
        id = parseFrameData<Message>(incoming).id
    }
    return id!!
}