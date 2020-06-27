package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.graphql.operations.subscriptions.receiveMessageUpdates
import graphql.operations.mutations.createMessage
import graphql.operations.subscriptions.parseFrameData

/** Has the user who bears the [accessToken] send a [text] in the [chatId], and returns the message's ID. */
fun messageAndReadId(accessToken: String, chatId: Int, text: String): Int {
    var id: Int? = null
    receiveMessageUpdates(accessToken, chatId) { incoming, _ ->
        createMessage(accessToken, chatId, text)
        id = parseFrameData<Message>(incoming).id
    }
    return id!!
}