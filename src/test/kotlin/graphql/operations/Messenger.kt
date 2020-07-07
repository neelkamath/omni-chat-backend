package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.NewMessage
import com.neelkamath.omniChat.TextMessage
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.subscriptions.parseFrameData
import com.neelkamath.omniChat.graphql.operations.subscriptions.subscribeToMessages

/**
 * Has the user who bears the [accessToken] send the [text] [count] times in the [chatId], and returns the its ID.
 *
 * @see [messageAndReadId]
 */
fun messageAndReadIdList(accessToken: String, chatId: Int, text: TextMessage, count: Int): List<Int> {
    val list = mutableListOf<Int>()
    subscribeToMessages(accessToken) { incoming ->
        repeat(count) {
            createMessage(accessToken, chatId, text)
            parseFrameData<NewMessage>(incoming).messageId.let(list::add)
        }
    }
    return list
}

/** Convenience function for [messageAndReadIdList] */
fun messageAndReadId(accessToken: String, chatId: Int, text: TextMessage): Int =
    messageAndReadIdList(accessToken, chatId, text, count = 1)[0]