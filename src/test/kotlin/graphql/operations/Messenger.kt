package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.NewMessage
import com.neelkamath.omniChat.TextMessage
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.subscriptions.parseFrameData
import com.neelkamath.omniChat.graphql.operations.subscriptions.subscribeToMessages

/** Has the user who bears the [accessToken] send a [text] in the [chatId], and returns the message's ID. */
fun messageAndReadId(accessToken: String, chatId: Int, text: TextMessage): Int {
    var id: Int? = null
    subscribeToMessages(accessToken) { incoming ->
        createMessage(accessToken, chatId, text)
        id = parseFrameData<NewMessage>(incoming).messageId
    }
    return id!!
}