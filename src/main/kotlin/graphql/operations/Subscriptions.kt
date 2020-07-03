package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.db.tables.PrivateChats
import com.neelkamath.omniChat.graphql.InvalidChatIdException
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher

fun subscribeToMessages(env: DataFetchingEnvironment): Publisher<MessagesSubscription> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    return messagesBroker.subscribe(MessagesAsset(env.userId!!, chatId))
}

fun subscribeToContacts(env: DataFetchingEnvironment): Publisher<ContactsSubscription> {
    env.verifyAuth()
    return contactsBroker.subscribe(ContactsAsset(env.userId!!))
}

fun subscribeToPrivateChatInfo(env: DataFetchingEnvironment): Publisher<PrivateChatInfoSubscription> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val users = PrivateChats.readUsers(chatId)
    val userId = if (users[0] == env.userId!!) users[1] else users[0]
    return privateChatInfoBroker
        .subscribe(PrivateChatInfoAsset(subscriberId = env.userId!!, userId = userId))
}

fun subscribeToGroupChatInfo(env: DataFetchingEnvironment): Publisher<GroupChatInfoSubscription> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    return groupChatInfoBroker.subscribe(GroupChatInfoAsset(chatId, env.userId!!))
}