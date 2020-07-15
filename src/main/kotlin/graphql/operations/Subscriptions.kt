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

fun subscribeToUpdatedChats(env: DataFetchingEnvironment): Publisher<UpdatedChatsSubscription> {
    env.verifyAuth()
    return updatedChatsBroker.subscribe(UpdatedChatsAsset(env.userId!!))
}

fun subscribeToNewGroupChats(env: DataFetchingEnvironment): Publisher<NewGroupChatsSubscription> {
    env.verifyAuth()
    return newGroupChatsBroker.subscribe(NewGroupChatsAsset(env.userId!!))
}

fun subscribeToTypingStatuses(env: DataFetchingEnvironment): Publisher<TypingStatusesSubscription> {
    env.verifyAuth()
    return typingStatusesBroker.subscribe(TypingStatusesAsset(env.userId!!))
}