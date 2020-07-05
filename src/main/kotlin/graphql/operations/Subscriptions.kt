package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher

fun subscribeToMessages(env: DataFetchingEnvironment): Publisher<MessagesSubscription> {
    env.verifyAuth()
    return messagesBroker.subscribe(MessagesAsset(env.userId!!))
}

fun subscribeToContacts(env: DataFetchingEnvironment): Publisher<ContactsSubscription> {
    env.verifyAuth()
    return contactsBroker.subscribe(ContactsAsset(env.userId!!))
}

fun subscribeToPrivateChatInfo(env: DataFetchingEnvironment): Publisher<PrivateChatInfoSubscription> {
    env.verifyAuth()
    return privateChatInfoBroker.subscribe(PrivateChatInfoAsset(env.userId!!))
}

fun subscribeToGroupChatInfo(env: DataFetchingEnvironment): Publisher<GroupChatInfoSubscription> {
    env.verifyAuth()
    return groupChatInfoBroker.subscribe(GroupChatInfoAsset(env.userId!!))
}

fun subscribeToNewGroupChats(env: DataFetchingEnvironment): Publisher<NewGroupChatsSubscription> {
    env.verifyAuth()
    return newGroupChatsBroker.subscribe(NewGroupChatsAsset(env.userId!!))
}