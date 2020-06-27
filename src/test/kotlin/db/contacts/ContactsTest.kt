package com.neelkamath.omniChat.db.contacts

import com.neelkamath.omniChat.DeletedContact
import com.neelkamath.omniChat.NewContact
import com.neelkamath.omniChat.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class ContactsTest : FunSpec({
    context("create(String, Set<String>)") {
        test("Saving contacts should ignore existing contacts") {
            val (userId, contact1Id, contact2Id, contact3Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(userId, setOf(contact1Id, contact2Id))
            Contacts.create(userId, setOf(contact1Id, contact2Id, contact3Id))
            Contacts.readIdList(userId) shouldBe listOf(contact1Id, contact2Id, contact3Id)
        }

        test("When the subscriber saves new and old contacts, they should only be notified of the new ones") {
            val (ownerId, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(ownerId, setOf(user2Id, user3Id))
            val subscriber = subscribeToContactUpdates(ownerId).subscribeWith(TestSubscriber())
            Contacts.create(ownerId, setOf(user3Id, user4Id))
            subscriber.assertValue(NewContact.buildFromUserId(user4Id))
        }
    }

    context("delete(String, List<String>)") {
        test(
            """
            Given saved contacts, duplicate contacts, unsaved contacts, and invalid contacts,
            when the subscriber deletes them,
            then they should only be notified of the deletion of unique saved contacts
            """
        ) {
            val (ownerId, contact1Id, contact2Id, unsavedContactId) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(ownerId, setOf(contact1Id, contact2Id))
            val subscriber = subscribeToContactUpdates(ownerId).subscribeWith(TestSubscriber())
            Contacts.delete(ownerId, listOf(contact1Id, contact1Id, contact2Id, unsavedContactId, "invalid ID"))
            subscriber.assertValues(DeletedContact(contact1Id), DeletedContact(contact2Id))
        }
    }
})