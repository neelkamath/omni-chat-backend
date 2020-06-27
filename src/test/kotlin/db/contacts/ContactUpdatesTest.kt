package com.neelkamath.omniChat.db.contacts

import com.neelkamath.omniChat.AccountUpdate
import com.neelkamath.omniChat.UpdatedContact
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.updateUser
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.subscribers.TestSubscriber

class ContactUpdatesTest : FunSpec({
    context("notify(String, ContactUpdate)") {
        test("When there's a contact update, only the relevant subscribers should be notified") {
            val (owner1Id, owner2Id, userId, contactId) = createVerifiedUsers(4).map { it.info.id }
            listOf(owner1Id, owner2Id).map { Contacts.create(it, setOf(contactId)) }
            updateUser(contactId, AccountUpdate("new_username"))
            val (owner1Subscriber, owner2Subscriber, userSubscriber) =
                listOf(owner1Id, owner2Id, userId).map { subscribeToContactUpdates(it).subscribeWith(TestSubscriber()) }
            notifyOfUpdatedContact(contactId)
            listOf(owner1Subscriber, owner2Subscriber)
                .forEach { it.assertValue(UpdatedContact.buildFromUserId(contactId)) }
            userSubscriber.assertNoValues()
        }
    }
})