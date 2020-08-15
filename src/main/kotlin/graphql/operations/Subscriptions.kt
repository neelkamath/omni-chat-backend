package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.userId
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher

fun subscribeToMessages(env: DataFetchingEnvironment): Publisher<MessagesSubscription> {
    env.verifyAuth()
    return messagesNotifier.subscribe(MessagesAsset(env.userId!!))
}

fun subscribeToAccounts(env: DataFetchingEnvironment): Publisher<AccountsSubscription> {
    env.verifyAuth()
    return accountsNotifier.subscribe(AccountsAsset(env.userId!!))
}

fun subscribeToGroupChats(env: DataFetchingEnvironment): Publisher<GroupChatsSubscription> {
    env.verifyAuth()
    return groupChatsNotifier.subscribe(GroupChatsAsset(env.userId!!))
}

fun subscribeToTypingStatuses(env: DataFetchingEnvironment): Publisher<TypingStatusesSubscription> {
    env.verifyAuth()
    return typingStatusesNotifier.subscribe(TypingStatusesAsset(env.userId!!))
}

fun subscribeToOnlineStatuses(env: DataFetchingEnvironment): Publisher<OnlineStatusesSubscription> {
    env.verifyAuth()
    return onlineStatusesNotifier.subscribe(OnlineStatusesAsset(env.userId!!))
}