package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.ContactUpdate
import com.neelkamath.omniChat.MessageUpdate
import com.neelkamath.omniChat.db.contacts.subscribeToContactUpdates
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.messages.subscribeToMessageUpdates
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import com.neelkamath.omniChat.userId
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher

fun operateMessageUpdates(env: DataFetchingEnvironment): Publisher<MessageUpdate> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    return subscribeToMessageUpdates(env.userId!!, chatId)
}

fun operateContactUpdates(env: DataFetchingEnvironment): Publisher<ContactUpdate> {
    env.verifyAuth()
    return subscribeToContactUpdates(env.userId!!)
}