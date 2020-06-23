package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.MessageUpdates
import com.neelkamath.omniChat.userId
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

private val notifiers = mutableListOf<Notifier>()

/** Used to make [Notifier.id]s. Increment every usage to get a unique ID. */
private var notifierId = 0

/** The [userId] will be notified whenever a message has been created or deleted in the [chatId]. */
private data class Notifier(val userId: String, val chatId: Int, val subject: Subject<MessageUpdates>) {
    /** This is useful because the [userId] may have the [chatId] open on multiple devices at the same time. */
    val id: Int = notifierId++
}

/** Publishes [chatId] [MessageUpdates]s. */
fun subscribeToMessageUpdates(userId: String, chatId: Int): Flowable<MessageUpdates> {
    val subject = PublishSubject.create<MessageUpdates>()
    val notifier = Notifier(userId, chatId, subject)
    notifiers.add(notifier)
    return subject
        .doFinally {
            notifiers.removeIf { it.id == notifier.id }
        }
        .toFlowable(BackpressureStrategy.BUFFER)
}

/** Stops notifications for the specified [userId] in the [chatId] by calling [Observer.onComplete]. */
fun unsubscribeUserFromMessageUpdates(userId: String, chatId: Int): Unit =
    unsubscribe { it.userId == userId && it.chatId == chatId }

/** Removes every [chatId] subscriber by calling [Observer.onComplete]. */
fun unsubscribeUsersFromMessageUpdates(chatId: Int): Unit = unsubscribe { it.chatId == chatId }

/** Depending on the [condition], [notifiers] are removed after calling [Observer.onComplete]. */
private inline fun unsubscribe(condition: (Notifier) -> Boolean): Unit =
    /*
    Another function removes the notifier from the list once it completes. This means we can't write
    <notifiers.forEach { if (condition) it.subject.onComplete() }> because a <ConcurrentModificationException> would
    get thrown.
     */
    notifiers.filter(condition).forEach { it.subject.onComplete() }

/** Notifies [subscribeToMessageUpdates]rs of the updated [Message]. */
fun notifyMessageUpdate(messageId: Int) {
    val chatId = Messages.readChatFromMessage(messageId)
    notifyMessageUpdate(chatId, Messages.read(messageId))
}

/** Sends the [chatId]'s [update] to the [notifiers]. */
fun notifyMessageUpdate(chatId: Int, update: MessageUpdates): Unit =
    notifiers.forEach { if (it.chatId == chatId) it.subject.onNext(update) }