package com.neelkamath.omniChat.db.messages

import com.neelkamath.omniChat.Message
import com.neelkamath.omniChat.MessageUpdate
import com.neelkamath.omniChat.userId
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject

/** Used to make [Notifier.id]s. Increment every usage to get a unique ID. */
private var notifierId = 0

private val notifiers: MutableList<Notifier> = mutableListOf()

private data class Notifier(val userId: String, val chatId: Int, val subject: Subject<MessageUpdate>) {
    /** An ID is required because the [userId] may have subscribed to the same [chatId] on multiple devices. */
    val id: Int = notifierId++
}

fun subscribeToMessageUpdates(userId: String, chatId: Int): Flowable<MessageUpdate> {
    val subject = PublishSubject.create<MessageUpdate>()
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
fun unsubscribeUsersFromMessageUpdates(chatId: Int): Unit =
    unsubscribe { it.chatId == chatId }

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
fun notifyMessageUpdate(chatId: Int, update: MessageUpdate): Unit =
    notifiers.forEach { if (it.chatId == chatId) it.subject.onNext(update) }