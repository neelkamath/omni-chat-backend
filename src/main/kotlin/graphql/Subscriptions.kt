package com.neelkamath.omniChat.graphql

import com.neelkamath.omniChat.MessageUpdate
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.subscribeToMessageUpdates
import com.neelkamath.omniChat.userId
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher

fun operateMessageUpdates(env: DataFetchingEnvironment): Publisher<MessageUpdate> {
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException()
    return subscribeToMessageUpdates(env.userId!!, chatId)
}