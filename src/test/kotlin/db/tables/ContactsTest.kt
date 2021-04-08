package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.accountsNotifier
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.graphql.routing.DeletedContact
import com.neelkamath.omniChatBackend.graphql.routing.NewContact
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

fun Contacts.createAll(ownerId: Int, contactIdList: Set<Int>): Unit = contactIdList.forEach { create(ownerId, it) }

@ExtendWith(DbExtension::class)
class ContactsTest {
    @Nested
    inner class Create {
        @Test
        fun `Saving a contact must notify subscribers`(): Unit = runBlocking {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            val subscriber = accountsNotifier.subscribe(ownerId).subscribeWith(TestSubscriber())
            assertTrue(Contacts.create(ownerId, contactId))
            awaitBrokering()
            subscriber.assertValue(NewContact.build(contactId))
        }

        @Test
        fun `Saving a previously saved contact mustn't notify subscribers`(): Unit = runBlocking {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            Contacts.create(ownerId, contactId)
            awaitBrokering()
            val subscriber = accountsNotifier.subscribe(ownerId).subscribeWith(TestSubscriber())
            assertFalse(Contacts.create(ownerId, contactId))
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    @Nested
    inner class ReadOwners {
        @Test
        fun `Every contact owner must be read`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val owners = listOf(user2Id, user3Id).toSet()
            owners.forEach { Contacts.create(it, user1Id) }
            assertEquals(owners, Contacts.readOwners(user1Id))
            owners.forEach { assertTrue(Contacts.readOwners(it).isEmpty()) }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting a contact must notify subscribers`(): Unit = runBlocking {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            Contacts.create(ownerId, contactId)
            awaitBrokering()
            val subscriber = accountsNotifier.subscribe(ownerId).subscribeWith(TestSubscriber())
            Contacts.delete(ownerId, contactId)
            awaitBrokering()
            subscriber.assertValue(DeletedContact(contactId))
        }

        @Test
        fun `Deleting a nonexistent contact mustn't notify subscribers`(): Unit = runBlocking {
            val userId = createVerifiedUsers(1).first().info.id
            val subscriber = accountsNotifier.subscribe(userId).subscribeWith(TestSubscriber())
            Contacts.delete(userId, contactId = -1)
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    @Nested
    inner class DeleteUserEntries {
        @Test
        fun `Deleting a user must notify only users who have them in their contacts`(): Unit = runBlocking {
            val (ownerId, contactId, userId) = createVerifiedUsers(3).map { it.info.id }
            Contacts.create(ownerId, contactId)
            awaitBrokering()
            val (ownerSubscriber, contactSubscriber, userSubscriber) = listOf(ownerId, contactId, userId)
                .map { accountsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            Contacts.deleteUserEntries(contactId)
            awaitBrokering()
            ownerSubscriber.assertValue(DeletedContact(contactId))
            listOf(contactSubscriber, userSubscriber).forEach { it.assertNoValues() }
        }
    }
}
