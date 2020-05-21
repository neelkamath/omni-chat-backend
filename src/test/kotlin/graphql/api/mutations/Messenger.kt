package com.neelkamath.omniChat.test.graphql.api.mutations

import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.test.CreatedUser
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.subscriptions.operateMessageUpdates
import com.neelkamath.omniChat.test.graphql.api.subscriptions.parseFrameData

/** A private chat between two users where [user2] sent the [messageId]. */
data class UtilizedPrivateChat(
    val messageId: Int,
    val user1: CreatedUser,
    val user2: CreatedUser
)

fun createUtilizedPrivateChat(): UtilizedPrivateChat {
    val (user1, user2) = createVerifiedUsers(2)
    val chatId = createPrivateChat(user2.info.id, user1.accessToken)
    val messageId = readCreatedMessageId(chatId, "text", user2.accessToken)
    return UtilizedPrivateChat(messageId, user1, user2)
}

/** Has the authenticated user sent the [text] in the [chatId], and returns the message's ID. */
fun readCreatedMessageId(chatId: Int, text: String, accessToken: String): Int {
    var id: Int? = null
    operateMessageUpdates(chatId, accessToken) { incoming, _ ->
        createMessage(chatId, text, accessToken)
        id = parseFrameData<Message>(incoming).id
    }
    return id!!
}