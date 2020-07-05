package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * [subscribe] to be [notify]d of [U]s, and [unsubscribe] once you're done. The [T] is used to filter which [Flowable]s
 * get [notify]d.
 */
class Broker<T, U> {
    /** List of [subscribe]rs which should only be mutated in [subscribe] and [unsubscribe]. */
    private val notifiers: MutableList<Notifier<T, U>> = mutableListOf()

    private data class Notifier<T, U>(val data: T, val subject: PublishSubject<U>) {
        /** Guaranteed to be unique for every [Notifier]. */
        val id: Int = notifierId++
    }

    /** @param[data] used to filter which clients will get [notify]d. */
    fun subscribe(data: T): Flowable<U> {
        val subject = PublishSubject.create<U>()
        val notifier = Notifier(data, subject)
        notifiers.add(notifier)
        return subject
            .doFinally {
                notifiers.removeIf { it.id == notifier.id }
            }
            .toFlowable(BackpressureStrategy.BUFFER)
    }

    /** Sends the [update] to the [filter]ed [subscribe]rs. */
    fun notify(update: U, filter: (T) -> Boolean): Unit =
        notifiers.forEach { if (filter(it.data)) it.subject.onNext(update) }

    /** Removes [filter]ed subscribers after calling [Observer.onComplete]. */
    fun unsubscribe(filter: (T) -> Boolean): Unit =
        /*
        <subscribe()> removes the notifier from the list once it completes. This means we can't write
        <notifiers.forEach { if (condition) it.subject.onComplete() }> because a <ConcurrentModificationException> would
        get thrown.
         */
        notifiers.filter { filter(it.data) }.forEach { it.subject.onComplete() }

    private companion object {
        /** Used to make [Notifier.id]s. Increment every usage to get a unique ID. */
        private var notifierId = 0
    }
}

data class MessagesAsset(val userId: String)

val messagesBroker = Broker<MessagesAsset, MessagesSubscription>()

data class ContactsAsset(val userId: String)

val contactsBroker = Broker<ContactsAsset, ContactsSubscription>()

data class PrivateChatInfoAsset(val userId: String)

val privateChatInfoBroker = Broker<PrivateChatInfoAsset, PrivateChatInfoSubscription>()

data class GroupChatInfoAsset(val userId: String)

val groupChatInfoBroker = Broker<GroupChatInfoAsset, GroupChatInfoSubscription>()

data class NewGroupChatsAsset(val userId: String)

val newGroupChatsBroker = Broker<NewGroupChatsAsset, NewGroupChatsSubscription>()