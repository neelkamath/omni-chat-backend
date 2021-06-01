@file:Suppress("BlockingMethodInNonBlockingContext")

package com.neelkamath.omniChatBackend.graphql.operations

import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.graphql.engine.verifyAuth
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
import org.reactivestreams.Publisher
import java.util.*
import kotlin.concurrent.schedule

fun subscribeToMessages(env: DataFetchingEnvironment): Publisher<MessagesSubscription> {
    env.verifyAuth()
    return runBlocking { messagesNotifier.subscribe(UserId(env.userId!!)).flowable }
}

fun subscribeToChatMessages(env: DataFetchingEnvironment): Publisher<ChatMessagesSubscription> =
    subscribeToChat(env, chatMessagesNotifier)

fun subscribeToChatAccounts(env: DataFetchingEnvironment): Publisher<ChatAccountsSubscription> =
    subscribeToChat(env, chatAccountsNotifier)

fun subscribeToAccounts(env: DataFetchingEnvironment): Publisher<AccountsSubscription> {
    env.verifyAuth()
    return runBlocking { accountsNotifier.subscribe(UserId(env.userId!!)).flowable }
}

fun subscribeToGroupChatMetadata(env: DataFetchingEnvironment): Publisher<GroupChatMetadataSubscription> =
    subscribeToChat(env, groupChatMetadataNotifier)

fun subscribeToGroupChats(env: DataFetchingEnvironment): Publisher<GroupChatsSubscription> {
    env.verifyAuth()
    return runBlocking { groupChatsNotifier.subscribe(UserId(env.userId!!)).flowable }
}

fun subscribeToTypingStatuses(env: DataFetchingEnvironment): Publisher<TypingStatusesSubscription> {
    env.verifyAuth()
    return runBlocking { typingStatusesNotifier.subscribe(UserId(env.userId!!)).flowable }
}

fun subscribeToChatTypingStatuses(env: DataFetchingEnvironment): Publisher<ChatTypingStatusesSubscription> =
    subscribeToChat(env, chatTypingStatusesNotifier)

fun subscribeToOnlineStatuses(env: DataFetchingEnvironment): Publisher<OnlineStatusesSubscription> {
    env.verifyAuth()
    return runBlocking { onlineStatusesNotifier.subscribe(UserId(env.userId!!)).flowable }
}

fun subscribeToChatOnlineStatuses(env: DataFetchingEnvironment): Publisher<ChatOnlineStatusesSubscription> =
    subscribeToChat(env, chatOnlineStatusesNotifier)

private fun <T> subscribeToChat(env: DataFetchingEnvironment, notifier: Notifier<T, ChatId>): Publisher<T> {
    val chatId = env.getArgument<Int>("chatId")
    val (flowable, id) = runBlocking { notifier.subscribe(ChatId(chatId)) }
    if (!GroupChats.isExistingPublicChat(chatId)) {
        /*
        This block gets executed after a delay because the WebSocket must get created before we can send the
        error, and close the connection.
         */
        Timer().schedule(1_000) {
            @Suppress("UNCHECKED_CAST") notifier.notifySubscriber(InvalidChatId as T, id)
            runBlocking {
                notifier.unsubscribe { it.id == id }
            }
        }
    }
    return flowable
}
