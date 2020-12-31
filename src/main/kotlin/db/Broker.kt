package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.objectMapper
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
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
    brokerAccounts()
    brokerGroupChats()
    brokerTypingStatuses()
    brokerOnlineStatuses()
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [messagesNotifier]. This is safe to call multiple times. */
private fun brokerMessages() {
    if (redisson.getTopic(Topic.MESSAGES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.MESSAGES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST") messagesNotifier.notify(message as List<Notification<MessagesSubscription>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [accountsNotifier]. This is safe to call multiple times. */
private fun brokerAccounts() {
    if (redisson.getTopic(Topic.ACCOUNTS.toString()).countListeners() == 0)
        redisson.getTopic(Topic.ACCOUNTS.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST") accountsNotifier.notify(message as List<Notification<AccountsSubscription>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [groupChatsNotifier]. This is safe to call multiple times. */
private fun brokerGroupChats() {
    if (redisson.getTopic(Topic.GROUP_CHATS.toString()).countListeners() == 0)
        redisson.getTopic(Topic.GROUP_CHATS.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST") groupChatsNotifier.notify(message as List<Notification<GroupChatsSubscription>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [typingStatusesNotifier]. Safe to call multiple times. */
private fun brokerTypingStatuses() {
    if (redisson.getTopic(Topic.TYPING_STATUSES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.TYPING_STATUSES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            typingStatusesNotifier.notify(message as List<Notification<TypingStatusesSubscription>>)
        }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [onlineStatusesNotifier]. Safe to call multiple times. */
private fun brokerOnlineStatuses() {
    if (redisson.getTopic(Topic.ONLINE_STATUSES.toString()).countListeners() == 0)
        redisson.getTopic(Topic.ONLINE_STATUSES.toString()).addListener(List::class.java) { _, message ->
            @Suppress("UNCHECKED_CAST")
            onlineStatusesNotifier.notify(message as List<Notification<OnlineStatusesSubscription>>)
        }
}

/** The message broker channel updates will be sent over to each server. */
enum class Topic {
    MESSAGES {
        override fun toString() = "messages"
    },
    ACCOUNTS {
        override fun toString() = "accounts"
    },
    GROUP_CHATS {
        override fun toString() = "groupChats"
    },
    TYPING_STATUSES {
        override fun toString() = "typingStatuses"
    },
    ONLINE_STATUSES {
        override fun toString() = "onlineStatuses"
    }
}

/** [subscribe]d clients with the [userId] will receive the corresponding [update]. */
data class Notification<T>(val userId: Int, val update: T)

/** [subscribe] to be [notify]d of updates. */
class Notifier<T>(private val topic: Topic) {
    /** List of [subscribe]rs which must only be mutated in [subscribe] and [unsubscribe]. */
    private val clients: MutableList<Client<T>> = mutableListOf()

    private data class Client<T>(val userId: Int, val subject: PublishSubject<T>) {
        /** Guaranteed to be unique for every [Client]. */
        val id: Int = clientId.incrementAndGet()
    }

    /** The [userId] is used to filter which clients will get [notify]d. */
    fun subscribe(userId: Int): Flowable<T> {
        val subject = PublishSubject.create<T>()
        val client = Client(userId, subject)
        clients.add(client)
        return subject
                .doFinally {
                    clients.removeIf { it.id == client.id }
                }
                .toFlowable(BackpressureStrategy.BUFFER)
    }

    /** Publishes [notifications] to the message broker which in turn [notify]s every server. */
    fun publish(notifications: List<Notification<T>>) {
        redisson.getTopic(topic.toString()).publish(notifications)
    }

    fun publish(notifications: Map<Int, T>): Unit = publish(notifications.map { Notification(it.key, it.value) })

    fun publish(vararg notifications: Pair<Int, T>): Unit =
            publish(notifications.map { Notification(it.first, it.second) })

    fun publish(update: T, subscribers: Collection<Int>): Unit = publish(subscribers.map { Notification(it, update) })

    fun publish(update: T, vararg subscribers: Int): Unit = publish(subscribers.map { Notification(it, update) })

    /**
     * This must only be called from [subscribeToMessageBroker]. Pass the [notifications] the message broker yielded to
     * [notify] subscribers. [publish] [notifications] to notify subscribers from outside [subscribeToMessageBroker].
     */
    fun notify(notifications: List<Notification<T>>): Unit = clients.forEach { client ->
        notifications.firstOrNull { it.userId == client.userId }?.let { client.subject.onNext(it.update) }
    }

    /** Removes [filter]ed subscribers after calling [Observer.onComplete]. */
    fun unsubscribe(filter: (Int) -> Boolean): Unit =
            /*
            <subscribe()> removes the notifier from the list once it completes. This means we can't write
            <notifiers.forEach { if (condition) it.subject.onComplete() }> because a <ConcurrentModificationException>
            would get thrown.
             */
            clients.filter { filter(it.userId) }.forEach { it.subject.onComplete() }

    private companion object {
        /** Used to create [Client.id]s. Increment every usage to get a unique ID. */
        private val clientId = AtomicInteger()
    }
}

val messagesNotifier = Notifier<MessagesSubscription>(Topic.MESSAGES)

/** @see [negotiateUserUpdate] */
val accountsNotifier = Notifier<AccountsSubscription>(Topic.ACCOUNTS)

val groupChatsNotifier = Notifier<GroupChatsSubscription>(Topic.GROUP_CHATS)

val typingStatusesNotifier = Notifier<TypingStatusesSubscription>(Topic.TYPING_STATUSES)

val onlineStatusesNotifier = Notifier<OnlineStatusesSubscription>(Topic.ONLINE_STATUSES)

/** Notifies subscribers of the updated [userId] via [accountsNotifier]. */
fun negotiateUserUpdate(userId: Int) {
    val subscribers = Contacts.readOwners(userId) + userId + readChatSharers(userId)
    accountsNotifier.publish(UpdatedAccount.build(userId), subscribers)
}
