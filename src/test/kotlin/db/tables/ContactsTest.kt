package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ContactsAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.contactsNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.DeletedContact
import com.neelkamath.omniChat.graphql.routing.NewContact
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class ContactsTest : FunSpec({
    context("create(Int, Set<Int>)") {
        test("Saving contacts should ignore existing contacts") {
            val (userId, contact1Id, contact2Id, contact3Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(userId, setOf(contact1Id, contact2Id))
            Contacts.create(userId, setOf(contact1Id, contact2Id, contact3Id))
            Contacts.readIdList(userId) shouldBe listOf(contact1Id, contact2Id, contact3Id)
        }

        test("When the subscriber saves new and old contacts, they should only be notified of the new ones") {
            val (ownerId, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(ownerId, setOf(user2Id, user3Id))
            val subscriber = contactsNotifier.safelySubscribe(ContactsAsset(ownerId)).subscribeWith(TestSubscriber())
            Contacts.create(ownerId, setOf(user3Id, user4Id))
            awaitBrokering()
            subscriber.assertValue(NewContact.build(user4Id))
        }
    }

    context("readOwners(Int)") {
        test("Every contact owner should be read") {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val owners = listOf(user2Id, user3Id)
            owners.forEach { Contacts.create(it, setOf(user1Id)) }
            Contacts.readOwners(user1Id) shouldBe owners
            owners.forEach { Contacts.readOwners(it).shouldBeEmpty() }
        }
    }

    context("delete(Int, List<Int>)") {
        test(
            """
            Given saved contacts, duplicate contacts, unsaved contacts, and invalid contacts,
            when the subscriber deletes them,
            then they should only be notified of the deletion of unique saved contacts
            """
        ) {
            val (ownerId, contact1Id, contact2Id, unsavedContactId) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(ownerId, setOf(contact1Id, contact2Id))
            val subscriber = contactsNotifier.safelySubscribe(ContactsAsset(ownerId)).subscribeWith(TestSubscriber())
            Contacts.delete(ownerId, listOf(contact1Id, contact1Id, contact2Id, unsavedContactId, -1))
            awaitBrokering()
            subscriber.assertValues(DeletedContact(contact1Id), DeletedContact(contact2Id))
        }
    }

    context("deleteUserEntries(Int)") {
        test("Deleting a user should notify only users who have them in their contacts") {
            val (ownerId, contactId, userId) = createVerifiedUsers(3).map { it.info.id }
            Contacts.create(ownerId, setOf(contactId))
            val (ownerSubscriber, contactSubscriber, userSubscriber) = listOf(ownerId, contactId, userId)
                .map { contactsNotifier.safelySubscribe(ContactsAsset(it)).subscribeWith(TestSubscriber()) }
            Contacts.deleteUserEntries(contactId)
            awaitBrokering()
            ownerSubscriber.assertValue(DeletedContact(contactId))
            listOf(contactSubscriber, userSubscriber).forEach { it.assertNoValues() }
        }
    }
})