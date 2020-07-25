package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DeletedContact
import com.neelkamath.omniChat.NewContact
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ContactsAsset
import com.neelkamath.omniChat.db.contactsBroker
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
            val subscriber = contactsBroker.subscribe(ContactsAsset(ownerId)).subscribeWith(TestSubscriber())
            Contacts.create(ownerId, setOf(user3Id, user4Id))
            subscriber.assertValue(NewContact.build(user4Id))
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
            val subscriber = contactsBroker.subscribe(ContactsAsset(ownerId)).subscribeWith(TestSubscriber())
            Contacts.delete(ownerId, listOf(contact1Id, contact1Id, contact2Id, unsavedContactId, -1))
            subscriber.assertValues(DeletedContact(contact1Id), DeletedContact(contact2Id))
        }
    }

    context("deleteUserEntries(String)") {
        test("Deleting a user should notify only users who have them in their contacts") {
            val (ownerId, contactId, userId) = createVerifiedUsers(3).map { it.info.id }
            Contacts.create(ownerId, setOf(contactId))
            val (ownerSubscriber, contactSubscriber, userSubscriber) = listOf(ownerId, contactId, userId)
                .map { contactsBroker.subscribe(ContactsAsset(it)).subscribeWith(TestSubscriber()) }
            Contacts.deleteUserEntries(contactId)
            ownerSubscriber.assertValue(DeletedContact(contactId))
            listOf(contactSubscriber, userSubscriber).forEach { it.assertNoValues() }
        }
    }
})