package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.db.tables.Contacts
import com.neelkamath.omniChatBackend.db.tables.GroupChatUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.objectMapper
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import java.util.concurrent.atomic.AtomicInteger

private val redisson: RedissonClient = Redisson.create(
    Config().apply {
        useSingleServer().address = System.getenv("REDIS_URL")
        codec = JsonJacksonCodec(objectMapper)
    }
)

/** [Notifier.notify]s all [Notifier.publish]ed notifications. This is safe to call multiple times. */
fun subscribeToMessageBroker() {
    brokerMessages()
    brokerChatMessages()
    brokerAccounts()
    brokerChats()
    brokerGroupChatMetadata()
    brokerChatAccounts()
    brokerChatTypingStatuses()
    brokerTypingStatuses()
    brokerOnlineStatuses()
    brokerChatOnlineStatuses()
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [messagesNotifier]. This is safe to call multiple times. */
private fun brokerMessages() {
    if (redisson.getTopic(Topic.MESSAGES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.MESSAGES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            messagesNotifier.notify(message as List<Notification<MessagesSubscription, UserId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [chatMessagesNotifier]. This is safe to call multiple times. */
private fun brokerChatMessages() {
    if (redisson.getTopic(Topic.CHAT_MESSAGES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.CHAT_MESSAGES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            chatMessagesNotifier.notify(message as List<Notification<ChatMessagesSubscription, ChatId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [accountsNotifier]. This is safe to call multiple times. */
private fun brokerAccounts() {
    if (redisson.getTopic(Topic.ACCOUNTS.toString()).countListeners() == 0)
        redisson.getTopic(Topic.ACCOUNTS.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            accountsNotifier.notify(message as List<Notification<AccountsSubscription, UserId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [chatAccountsNotifier]. This is safe to call multiple times. */
private fun brokerChatAccounts() {
    if (redisson.getTopic(Topic.CHAT_ACCOUNTS.toString()).countListeners() == 0)
        redisson.getTopic(Topic.CHAT_ACCOUNTS.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            chatAccountsNotifier.notify(message as List<Notification<ChatAccountsSubscription, ChatId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [chatsNotifier]. This is safe to call multiple times. */
private fun brokerChats() {
    if (redisson.getTopic(Topic.CHATS.toString()).countListeners() == 0)
        redisson.getTopic(Topic.CHATS.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            chatsNotifier.notify(message as List<Notification<ChatsSubscription, UserId>>)
        }
}

/**
 * [Notifier.notify]s [Notifier.publish]ed updates for [groupChatMetadataNotifier]. This is safe to call multiple times.
 */
private fun brokerGroupChatMetadata() {
    if (redisson.getTopic(Topic.GROUP_CHAT_METADATA.toString()).countListeners() == 0)
        redisson.getTopic(Topic.GROUP_CHAT_METADATA.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            groupChatMetadataNotifier.notify(message as List<Notification<GroupChatMetadataSubscription, ChatId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [typingStatusesNotifier]. Safe to call multiple times. */
private fun brokerTypingStatuses() {
    if (redisson.getTopic(Topic.TYPING_STATUSES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.TYPING_STATUSES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            typingStatusesNotifier.notify(message as List<Notification<TypingStatusesSubscription, UserId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [chatTypingStatusesNotifier]. Safe to call multiple times. */
private fun brokerChatTypingStatuses() {
    if (redisson.getTopic(Topic.CHAT_TYPING_STATUSES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.CHAT_TYPING_STATUSES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            chatTypingStatusesNotifier.notify(message as List<Notification<ChatTypingStatusesSubscription, ChatId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [onlineStatusesNotifier]. Safe to call multiple times. */
private fun brokerOnlineStatuses() {
    if (redisson.getTopic(Topic.ONLINE_STATUSES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.ONLINE_STATUSES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            onlineStatusesNotifier.notify(message as List<Notification<OnlineStatusesSubscription, UserId>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [chatOnlineStatusesNotifier]. Safe to call multiple times. */
private fun brokerChatOnlineStatuses() {
    if (redisson.getTopic(Topic.CHAT_ONLINE_STATUSES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.CHAT_ONLINE_STATUSES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            chatOnlineStatusesNotifier.notify(message as List<Notification<ChatOnlineStatusesSubscription, ChatId>>)
        }
}

/** The message broker channel updates will be sent over to each server. */
enum class Topic {
    MESSAGES {
        override fun toString() = "messages"
    },
    CHAT_MESSAGES {
        override fun toString() = "chatMessages"
    },
    ACCOUNTS {
        override fun toString() = "accounts"
    },
    CHAT_ACCOUNTS {
        override fun toString() = "chatAccounts"
    },
    CHATS {
        override fun toString() = "chats"
    },
    GROUP_CHAT_METADATA {
        override fun toString() = "groupChatMetadata"
    },
    TYPING_STATUSES {
        override fun toString() = "typingStatuses"
    },
    CHAT_TYPING_STATUSES {
        override fun toString() = "chatTypingStatuses"
    },
    ONLINE_STATUSES {
        override fun toString() = "onlineStatuses"
    },
    CHAT_ONLINE_STATUSES {
        override fun toString() = "chatOnlineStatuses"
    }
}

/** Clients [Notifier.subscribe]d with the [data] will receive the [update]. */
data class Notification<T, D : ClientData>(val update: T, val data: D)

interface ClientData

data class UserId(val userId: Int) : ClientData

data class ChatId(val chatId: Int) : ClientData

/** Each subscription generated by a particular server's run is associated with a unique integer. */
typealias SubscriptionId = Int

/** [subscribe] to be [notify]d of updates. */
class Notifier<T, D : ClientData>(private val topic: Topic) {
    /** List of [subscribe]rs which must only be mutated in [subscribe] and [unsubscribe]. */
    private val clients: MutableSet<Client<T, D>> = mutableSetOf()

    private val mutex = Mutex()

    data class DataWithId<D>(
        /** This is what the client used to [subscribe] with/ */
        val data: D,
        /** The [FlowableWithId.id] returned by [subscribe]. */
        val id: SubscriptionId,
    )

    private data class Client<T, D : ClientData>(val subject: PublishSubject<T>, private val data: D) {
        val metadata = DataWithId(data, clientId.incrementAndGet())
    }

    data class FlowableWithId<T>(val flowable: Flowable<T>, val id: SubscriptionId)

    /** The [data] is used to filter which clients will get [notify]d. */
    suspend fun subscribe(data: D): FlowableWithId<T> {
        val subject = PublishSubject.create<T>()
        val client = Client(subject, data)
        mutex.withLock { clients.add(client) }
        val flowable = subject
            .doFinally {
                clients.removeIf { it.metadata.id == client.metadata.id }
            }
            .toFlowable(BackpressureStrategy.BUFFER)
        return FlowableWithId(flowable, client.metadata.id)
    }

    /**
     * Notifies the [subscribers] of the [update] by publishing it to the message broker which in turn [notify]s every
     * server.
     */
    fun publish(update: T, subscribers: Collection<D>) {
        redisson.getTopic(topic.toString()).publish(subscribers.map { Notification(update, it) })
    }

    fun publish(update: T, vararg subscribers: D): Unit = publish(update, subscribers.toList())

    /**
     * This must only be called from [subscribeToMessageBroker]. Pass the [notifications] the message broker yielded to
     * [notify] subscribers. [publish] [notifications] to notify subscribers from outside [subscribeToMessageBroker].
     *
     * @see notifySubscriber
     */
    fun notify(notifications: Collection<Notification<T, D>>): Unit = clients.forEach { client ->
        notifications.forEach { if (it.data == client.metadata.data) client.subject.onNext(it.update) }
    }

    /**
     * Instead of calling [publish] to indirectly call [notify], you can call this function instead if you know the
     * subscriber is connected to this server. The [update] gets sent to the client with the [id].
     *
     * @see notify
     */
    fun notifySubscriber(update: T, id: SubscriptionId): Unit =
        clients.first { it.metadata.id == id }.subject.onNext(update)

    /** Removes [filter]ed subscribers after calling [Observer.onComplete]. */
    suspend fun unsubscribe(filter: (DataWithId<D>) -> Boolean): Unit =
        /*
        <subscribe()> removes the notifier from the list once it completes. This means we can't write
        <notifiers.forEach { if (condition) it.subject.onComplete() }> because a <ConcurrentModificationException>
        would get thrown.
         */
        mutex.withLock {
            clients.filter { filter(it.metadata) }.forEach { it.subject.onComplete() }
        }

    private companion object {
        /** Used to create the [Client.metadata]'s [DataWithId.id]s. Increment every usage to get a unique ID. */
        private val clientId = AtomicInteger()
    }
}

val messagesNotifier = Notifier<MessagesSubscription, UserId>(Topic.MESSAGES)

val chatMessagesNotifier = Notifier<ChatMessagesSubscription, ChatId>(Topic.CHAT_MESSAGES)

/** @see negotiateUserUpdate */
val accountsNotifier = Notifier<AccountsSubscription, UserId>(Topic.ACCOUNTS)

/** @see negotiateUserUpdate */
val chatAccountsNotifier = Notifier<ChatAccountsSubscription, ChatId>(Topic.CHAT_ACCOUNTS)

val chatsNotifier = Notifier<ChatsSubscription, UserId>(Topic.CHATS)

val groupChatMetadataNotifier = Notifier<GroupChatMetadataSubscription, ChatId>(Topic.GROUP_CHAT_METADATA)

val typingStatusesNotifier = Notifier<TypingStatusesSubscription, UserId>(Topic.TYPING_STATUSES)

val chatTypingStatusesNotifier = Notifier<ChatTypingStatusesSubscription, ChatId>(Topic.CHAT_TYPING_STATUSES)

val onlineStatusesNotifier = Notifier<OnlineStatusesSubscription, UserId>(Topic.ONLINE_STATUSES)

val chatOnlineStatusesNotifier = Notifier<ChatOnlineStatusesSubscription, ChatId>(Topic.CHAT_ONLINE_STATUSES)

/**
 * Notifies subscribers of the updated [userId] via [accountsNotifier] and [chatAccountsNotifier]. If [isProfileImage], an
 * [UpdatedProfileImage] will be sent, and an [UpdatedAccount] otherwise.
 */
fun negotiateUserUpdate(userId: Int, isProfileImage: Boolean) {
    val subscribers = Contacts.readOwnerUserIdList(userId).plus(userId).plus(readChatSharers(userId)).map(::UserId)
    val update = if (isProfileImage) UpdatedProfileImage(userId) else UpdatedAccount(userId)
    accountsNotifier.publish(update, subscribers)
    val chatIdList = GroupChatUsers.readChatIdList(userId).filter(GroupChats::isExistingPublicChat).map(::ChatId)
    chatAccountsNotifier.publish(update, chatIdList)
}
