package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ContactsAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.contactsNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.DeletedContact
import com.neelkamath.omniChat.graphql.routing.NewContact
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class ContactsTest {
    @Nested
    inner class Create {
        @Test
        fun `Saving contacts should ignore existing contacts`() {
            val (userId, contact1Id, contact2Id, contact3Id) = createVerifiedUsers(4).map { it.info.id }
            Contacts.create(userId, setOf(contact1Id, contact2Id))
            Contacts.create(userId, setOf(contact1Id, contact2Id, contact3Id))
            assertEquals(listOf(contact1Id, contact2Id, contact3Id), Contacts.readIdList(userId))
        }

        @Test
        fun `When the subscriber saves new and old contacts, they should only be notified of the new ones`() {
            runBlocking {
                val (ownerId, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
                Contacts.create(ownerId, setOf(user2Id, user3Id))
                val subscriber =
                    contactsNotifier.safelySubscribe(ContactsAsset(ownerId)).subscribeWith(TestSubscriber())
                Contacts.create(ownerId, setOf(user3Id, user4Id))
                awaitBrokering()
                subscriber.assertValue(NewContact.build(user4Id))
            }
        }
    }

    @Nested
    inner class ReadOwners {
        @Test
        fun `Every contact owner should be read`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val owners = listOf(user2Id, user3Id)
            owners.forEach { Contacts.create(it, setOf(user1Id)) }
            assertEquals(owners, Contacts.readOwners(user1Id))
            owners.forEach { assertTrue(Contacts.readOwners(it).isEmpty()) }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Only one notification should be sent for each deleted saved contact`() {
            runBlocking {
                val (ownerId, contact1Id, contact2Id, unsavedContactId) = createVerifiedUsers(4).map { it.info.id }
                Contacts.create(ownerId, setOf(contact1Id, contact2Id))
                val subscriber =
                    contactsNotifier.safelySubscribe(ContactsAsset(ownerId)).subscribeWith(TestSubscriber())
                Contacts.delete(ownerId, listOf(contact1Id, contact1Id, contact2Id, unsavedContactId, -1))
                awaitBrokering()
                subscriber.assertValues(DeletedContact(contact1Id), DeletedContact(contact2Id))
            }
        }
    }

    @Nested
    inner class DeleteUserEntries {
        @Test
        fun `Deleting a user should notify only users who have them in their contacts`(): Unit = runBlocking {
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
}