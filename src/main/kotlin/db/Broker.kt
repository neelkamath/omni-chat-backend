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

val redisson: RedissonClient = Redisson.create(
    Config().apply {
        useSingleServer().address = System.getenv("REDIS_URL")
        codec = JsonJacksonCodec(objectMapper)
    }
)

/** [Notifier.notify]s all [Notifier.publish]ed notifications. */
fun subscribeToMessageBroker() {
    brokerMessages()
    brokerAccounts()
    brokerGroupChats()
    brokerTypingStatuses()
    brokerOnlineStatuses()
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [messagesNotifier]. */
private fun brokerMessages() {
    redisson.getTopic(Topic.MESSAGES.toString()).addListener(List::class.java) { _, message ->
        @Suppress("UNCHECKED_CAST")
        messagesNotifier.notify(message as List<Notification<MessagesAsset, MessagesSubscription>>)
    }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [accountsNotifier]. */
private fun brokerAccounts() {
    redisson.getTopic(Topic.ACCOUNTS.toString()).addListener(List::class.java) { _, message ->
        @Suppress("UNCHECKED_CAST")
        accountsNotifier.notify(message as List<Notification<AccountsAsset, AccountsSubscription>>)
    }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [groupChatsNotifier]. */
private fun brokerGroupChats() {
    redisson.getTopic(Topic.GROUP_CHATS.toString()).addListener(List::class.java) { _, message ->
        @Suppress("UNCHECKED_CAST")
        groupChatsNotifier.notify(message as List<Notification<GroupChatsAsset, GroupChatsSubscription>>)
    }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [typingStatusesNotifier]. */
private fun brokerTypingStatuses() {
    redisson.getTopic(Topic.TYPING_STATUSES.toString()).addListener(List::class.java) { _, message ->
        @Suppress("UNCHECKED_CAST")
        typingStatusesNotifier.notify(message as List<Notification<TypingStatusesAsset, TypingStatusesSubscription>>)
    }
}

/** [Notifier.notify]s [Notifier.publish]ed updates for [onlineStatusesNotifier]. */
private fun brokerOnlineStatuses() {
    redisson.getTopic(Topic.ONLINE_STATUSES.toString()).addListener(List::class.java) { _, message ->
        @Suppress("UNCHECKED_CAST")
        onlineStatusesNotifier.notify(message as List<Notification<OnlineStatusesAsset, OnlineStatusesSubscription>>)
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

/** Clients who [subscribe]d with the [A]sset will receive the corresponding [U]pdate. */
data class Notification<A, U>(val asset: A, val update: U)

/** [subscribe] to be [notify]d of [U]pdates. The [A]sset is used to filter which [Flowable]s get [notify]d. */
class Notifier<A, U>(private val topic: Topic) {
    /** List of [subscribe]rs which must only be mutated in [subscribe] and [unsubscribe]. */
    private val clients: MutableList<Client<A, U>> = mutableListOf()

    private data class Client<A, U>(val asset: A, val subject: PublishSubject<U>) {
        /** Guaranteed to be unique for every [Client]. */
        val id: Int = clientId++
    }

    /** The [asset] is used to filter which clients will get [notify]d. */
    fun subscribe(asset: A): Flowable<U> {
        val subject = PublishSubject.create<U>()
        val client = Client(asset, subject)
        clients.add(client)
        return subject
            .doFinally {
                clients.removeIf { it.id == client.id }
            }
            .toFlowable(BackpressureStrategy.BUFFER)
    }

    /** Publishes [notifications] to the message broker which in turn [notify]s every server. */
    fun publish(notifications: List<Notification<A, U>>) {
        redisson.getTopic(topic.toString()).publish(notifications)
    }

    fun publish(notifications: Map<A, U>): Unit = publish(notifications.map { Notification(it.key, it.value) })

    fun publish(vararg notifications: Pair<A, U>): Unit =
        publish(notifications.map { Notification(it.first, it.second) })

    fun publish(update: U, subscribers: List<A>): Unit = publish(subscribers.map { Notification(it, update) })

    fun publish(update: U, vararg subscribers: A): Unit = publish(subscribers.map { Notification(it, update) })

    /**
     * This must only be called from [subscribeToMessageBroker]. Pass the [notifications] the message broker yielded to
     * [notify] subscribers. [publish] [notifications] to notify subscribers from outside [subscribeToMessageBroker].
     */
    fun notify(notifications: List<Notification<A, U>>): Unit = clients.forEach { client ->
        notifications.firstOrNull { it.asset == client.asset }?.let { client.subject.onNext(it.update) }
    }

    /** Removes [filter]ed subscribers after calling [Observer.onComplete]. */
    fun unsubscribe(filter: (A) -> Boolean): Unit =
        /*
        <subscribe()> removes the notifier from the list once it completes. This means we can't write
        <notifiers.forEach { if (condition) it.subject.onComplete() }> because a <ConcurrentModificationException>
        would get thrown.
         */
        clients.filter { filter(it.asset) }.forEach { it.subject.onComplete() }

    private companion object {
        /** Used to create [Client.id]s. Increment every usage to get a unique ID. */
        private var clientId = 0
    }
}

data class MessagesAsset(val userId: Int)

val messagesNotifier = Notifier<MessagesAsset, MessagesSubscription>(Topic.MESSAGES)

data class AccountsAsset(val userId: Int)

/** @see [negotiateUserUpdate] */
val accountsNotifier = Notifier<AccountsAsset, AccountsSubscription>(Topic.ACCOUNTS)

data class GroupChatsAsset(val userId: Int)

val groupChatsNotifier = Notifier<GroupChatsAsset, GroupChatsSubscription>(Topic.GROUP_CHATS)

data class TypingStatusesAsset(val userId: Int)

val typingStatusesNotifier = Notifier<TypingStatusesAsset, TypingStatusesSubscription>(Topic.TYPING_STATUSES)

data class OnlineStatusesAsset(val userId: Int)

val onlineStatusesNotifier = Notifier<OnlineStatusesAsset, OnlineStatusesSubscription>(Topic.ONLINE_STATUSES)

/** Notifies subscribers of the updated [userId] via [accountsNotifier]. */
fun negotiateUserUpdate(userId: Int) {
    val subscribers = Contacts.readOwners(userId) + userId + readChatSharers(userId)
    accountsNotifier.publish(UpdatedAccount.build(userId), subscribers.map(::AccountsAsset))
}