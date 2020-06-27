package com.neelkamath.omniChat.db.contacts

import com.neelkamath.omniChat.ContactUpdate
import com.neelkamath.omniChat.DeletedContact
import com.neelkamath.omniChat.UpdatedContact
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.subjects.PublishSubject

/** Used to make [Notifier.id]s. Increment every usage to get a unique ID. */
private var notifierId = 0

private val notifiers: MutableList<Notifier> = mutableListOf()

private data class Notifier(val userId: String, val subject: PublishSubject<ContactUpdate>) {
    /** An ID is required because the [userId] may be subscribed on multiple devices. */
    val id: Int = notifierId++
}

fun subscribeToContactUpdates(userId: String): Flowable<ContactUpdate> {
    val subject = PublishSubject.create<ContactUpdate>()
    val notifier = Notifier(userId, subject)
    notifiers.add(notifier)
    return subject
        .doFinally {
            notifiers.removeIf { it.id == notifier.id }
        }
        .toFlowable(BackpressureStrategy.BUFFER)
}

/** Every subscriber who has the [userId] in their contacts will be notified of their [UpdatedContact]. */
fun notifyOfUpdatedContact(userId: String): Unit = notify(userId, UpdatedContact.buildFromUserId(userId))

/** Every subscriber who has the [userId] in their contacts will be notified of their [DeletedContact]. */
fun notifyOfDeletedContact(userId: String): Unit = notify(userId, DeletedContact(userId))

/** Every subscriber who has the [userId] in their contacts will be notified of their [update]. */
private fun notify(userId: String, update: ContactUpdate): Unit =
    notifiers.forEach { if (userId in Contacts.readIdList(it.userId)) it.subject.onNext(update) }

/** Notifies the [userId] of the [update]. */
fun notifyContactUpdate(userId: String, update: ContactUpdate): Unit =
    notifiers.forEach { if (it.userId == userId) it.subject.onNext(update) }