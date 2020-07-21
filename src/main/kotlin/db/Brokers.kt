package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.Contacts
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject

interface Asset {
    val userId: Int
}

/**
 * [subscribe] to be [notify]d of updates ([U] for update), and [unsubscribe] once you're done. The [Asset] ([A] for
 * asset) is used to filter which [Flowable]s get [notify]d.
 */
class Broker<A : Asset, U> {
    /** List of [subscribe]rs which should only be mutated in [subscribe] and [unsubscribe]. */
    private val notifiers: MutableList<Notifier<A, U>> = mutableListOf()

    private data class Notifier<A, U>(val data: A, val subject: PublishSubject<U>) {
        /** Guaranteed to be unique for every [Notifier]. */
        val id: Int = notifierId++
    }

    /** @param[data] used to filter which clients will get [notify]d. */
    fun subscribe(data: A): Flowable<U> {
        val subject = PublishSubject.create<U>()
        val notifier = Notifier(data, subject)
        notifiers.add(notifier)
        return subject
            .doFinally {
                notifiers.removeIf { it.id == notifier.id }
            }
            .toFlowable(BackpressureStrategy.BUFFER)
    }

    /**
     * Sends the [update] to the [filter]ed [subscribe]rs. The [update] lambda is only executed if the [filter] returns
     * `true`.
     */
    fun notify(update: (A) -> U, filter: (A) -> Boolean): Unit =
        notifiers.forEach { if (filter(it.data)) it.subject.onNext(update(it.data)) }

    fun notify(update: U, filter: (A) -> Boolean): Unit = notify({ update }, filter)

    /** Removes [filter]ed subscribers after calling [Observer.onComplete]. */
    fun unsubscribe(filter: (A) -> Boolean): Unit =
        /*
        <subscribe()> removes the notifier from the list once it completes. This means we can't write
        <notifiers.forEach { if (condition) it.subject.onComplete() }> because a <ConcurrentModificationException>
        would get thrown.
         */
        notifiers.filter { filter(it.data) }.forEach { it.subject.onComplete() }

    private companion object {
        /** Used to make [Notifier.id]s. Increment every usage to get a unique ID. */
        private var notifierId = 0
    }
}

data class MessagesAsset(override val userId: Int) : Asset

val messagesBroker = Broker<MessagesAsset, MessagesSubscription>()

data class ContactsAsset(override val userId: Int) : Asset

/** @see [negotiateUserUpdate] */
val contactsBroker = Broker<ContactsAsset, ContactsSubscription>()

data class UpdatedChatsAsset(override val userId: Int) : Asset

/** @see [negotiateUserUpdate] */
val updatedChatsBroker = Broker<UpdatedChatsAsset, UpdatedChatsSubscription>()

data class NewGroupChatsAsset(override val userId: Int) : Asset

val newGroupChatsBroker = Broker<NewGroupChatsAsset, NewGroupChatsSubscription>()

data class TypingStatusesAsset(override val userId: Int) : Asset

val typingStatusesBroker = Broker<TypingStatusesAsset, TypingStatusesSubscription>()

data class OnlineStatusesAsset(override val userId: Int) : Asset

val onlineStatusesBroker = Broker<OnlineStatusesAsset, OnlineStatusesSubscription>()

/** [Broker.notify]s [Broker.subscribe]rs via [contactsBroker] and [updatedChatsBroker] of the updated [userId]. */
fun negotiateUserUpdate(userId: Int) {
    contactsBroker.notify(UpdatedContact.build(userId)) { userId in Contacts.readIdList(it.userId) }
    updatedChatsBroker.notify(UpdatedAccount.build(userId)) { it.userId != userId && shareChat(userId, it.userId) }
}