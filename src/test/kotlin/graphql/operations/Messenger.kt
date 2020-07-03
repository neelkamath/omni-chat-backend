package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.db.tables.TextMessage
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.subscriptions.parseFrameData
import com.neelkamath.omniChat.graphql.operations.subscriptions.subscribeToMessages

/** Has the user who bears the [accessToken] send a [text] in the [chatId], and returns the message's ID. */
fun messageAndReadId(accessToken: String, chatId: Int, text: TextMessage): Int {
    var id: Int? = null
    subscribeToMessages(accessToken, chatId) { incoming ->
        createMessage(accessToken, chatId, text)
        id = parseFrameData<Message>(incoming).id
    }
    return id!!
}