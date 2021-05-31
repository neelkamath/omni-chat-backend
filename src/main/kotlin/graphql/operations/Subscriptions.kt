package com.neelkamath.omniChatBackend.graphql.operations

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.graphql.engine.verifyAuth
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import java.util.*
import kotlin.concurrent.schedule

fun subscribeToMessages(env: DataFetchingEnvironment): Publisher<MessagesSubscription> {
    env.verifyAuth()
    return messagesNotifier.subscribe(UserId(env.userId!!))
}

fun subscribeToChatMessages(env: DataFetchingEnvironment): Publisher<ChatMessagesSubscription> =
    subscribeToChat(env, chatMessagesNotifier)

fun subscribeToAccounts(env: DataFetchingEnvironment): Publisher<AccountsSubscription> {
    env.verifyAuth()
    return accountsNotifier.subscribe(UserId(env.userId!!))
}

fun subscribeToGroupChats(env: DataFetchingEnvironment): Publisher<GroupChatsSubscription> {
    env.verifyAuth()
    return groupChatsNotifier.subscribe(UserId(env.userId!!))
}

fun subscribeToTypingStatuses(env: DataFetchingEnvironment): Publisher<TypingStatusesSubscription> {
    env.verifyAuth()
    return typingStatusesNotifier.subscribe(UserId(env.userId!!))
}

fun subscribeToOnlineStatuses(env: DataFetchingEnvironment): Publisher<OnlineStatusesSubscription> {
    env.verifyAuth()
    return onlineStatusesNotifier.subscribe(UserId(env.userId!!))
}

fun subscribeToChatOnlineStatuses(env: DataFetchingEnvironment): Publisher<ChatOnlineStatusesSubscription> =
    subscribeToChat(env, chatOnlineStatusesNotifier)

private fun <T> subscribeToChat(env: DataFetchingEnvironment, notifier: Notifier<T, ChatId>): Publisher<T> {
    val chatId = env.getArgument<Int>("chatId")
    val publisher = notifier.subscribe(ChatId(chatId))
    if (!GroupChats.isExistingPublicChat(chatId)) {
        /*
        This block gets executed after a delay because the WebSocket must get created before we can send the
        error, and close the connection.
         */
        Timer().schedule(1_000) {
            @Suppress("UNCHECKED_CAST") notifier.publish(InvalidChatId as T, ChatId(chatId))
            notifier.unsubscribe { it.chatId == chatId }
        }
    }
    return publisher
}
