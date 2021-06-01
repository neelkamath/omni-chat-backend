package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedContact
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.NewContact
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.toLinkedHashSet
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.test.*

fun Contacts.createAll(ownerUserId: Int, contactUserIdList: Collection<Int>): Unit =
    contactUserIdList.forEach { create(ownerUserId, it) }

@ExtendWith(DbExtension::class)
class ContactsTest {
    @Nested
    inner class Create {
        @Test
        fun `Saving a contact must notify subscribers`(): Unit = runBlocking {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.userId }
            val subscriber = accountsNotifier.subscribe(UserId(ownerId)).subscribeWith(TestSubscriber())
            assertTrue(Contacts.create(ownerId, contactId))
            awaitBrokering()
            val actual = subscriber.values().map { (it as NewContact).id }
            assertEquals(listOf(contactId), actual)
        }

        @Test
        fun `Saving a previously saved contact mustn't notify subscribers`(): Unit = runBlocking {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.userId }
            Contacts.create(ownerId, contactId)
            awaitBrokering()
            val subscriber = accountsNotifier.subscribe(UserId(ownerId)).subscribeWith(TestSubscriber())
            assertFalse(Contacts.create(ownerId, contactId))
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    /** The [contactUserIdList] (sorted in ascending order) saved by the [ownerUserId]. */
    private data class CreatedContacts(val ownerUserId: Int, val contactUserIdList: LinkedHashSet<Int>)

    private fun createContacts(count: Int = 10): CreatedContacts {
        val ownerUserId = createVerifiedUsers(1).first().userId
        val contactUserIdList = createVerifiedUsers(count)
            .map {
                Contacts.create(ownerUserId, it.userId)
                it.userId
            }
            .toLinkedHashSet()
        return CreatedContacts(ownerUserId, contactUserIdList)
    }

    @Nested
    inner class ReadIdList {
        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            assertEquals(contactUserIdList, Contacts.readIdList(ownerUserId))
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = contactUserIdList.elementAt(index))
            val actual = Contacts.readIdList(ownerUserId, pagination)
            assertEquals(contactUserIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            val first = 3
            val actual = Contacts.readIdList(ownerUserId, ForwardPagination(first))
            assertEquals(contactUserIdList.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            val index = 4
            val pagination = ForwardPagination(after = contactUserIdList.elementAt(index))
            val actual = Contacts.readIdList(ownerUserId, pagination)
            assertEquals(contactUserIdList.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            val pagination = ForwardPagination(after = contactUserIdList.last())
            assertEquals(0, Contacts.readIdList(ownerUserId, pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            Contacts.delete(ownerUserId, contactUserIdList.elementAt(3))
            val expected = listOf(2, 4, 5).map(contactUserIdList::elementAt).toLinkedHashSet()
            val pagination = ForwardPagination(first = 3, after = contactUserIdList.elementAt(1))
            assertEquals(expected, Contacts.readIdList(ownerUserId, pagination))
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            val index = 3
            Contacts.delete(ownerUserId, contactUserIdList.elementAt(index))
            val expected = contactUserIdList.drop(index + 1).toLinkedHashSet()
            val pagination = ForwardPagination(after = contactUserIdList.elementAt(index))
            assertEquals(expected, Contacts.readIdList(ownerUserId, pagination))
        }
    }

    @Nested
    inner class ReadCursor {
        @Test
        fun `If there are zero items, the start and end cursors must be 'null'`() {
            val ownerUserId = createVerifiedUsers(1).first().userId
            assertNull(Contacts.readCursor(ownerUserId, CursorType.START))
            assertNull(Contacts.readCursor(ownerUserId, CursorType.END))
        }

        @Test
        fun `The start and end cursors must point to the first and last items respectively`() {
            val (ownerUserId, contactUserIdList) = createContacts()
            assertEquals(contactUserIdList.first(), Contacts.readCursor(ownerUserId, CursorType.START))
            assertEquals(contactUserIdList.last(), Contacts.readCursor(ownerUserId, CursorType.END))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting a contact must notify subscribers`(): Unit = runBlocking {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.userId }
            Contacts.create(ownerId, contactId)
            awaitBrokering()
            val subscriber = accountsNotifier.subscribe(UserId(ownerId)).subscribeWith(TestSubscriber())
            assertTrue(Contacts.delete(ownerId, contactId))
            awaitBrokering()
            val actual = subscriber.values().map { (it as DeletedContact).getUserId() }
            assertEquals(listOf(contactId), actual)
        }

        @Test
        fun `Deleting a non-existing contact mustn't notify subscribers`(): Unit = runBlocking {
            val userId = createVerifiedUsers(1).first().userId
            val subscriber = accountsNotifier.subscribe(UserId(userId)).subscribeWith(TestSubscriber())
            assertFalse(Contacts.delete(userId, contactUserId = -1))
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    @Nested
    inner class DeleteUserEntries {
        @Test
        fun `Deleting a user must notify only users who have them in their contacts`(): Unit = runBlocking {
            val (ownerId, contactId, userId) = createVerifiedUsers(3).map { it.userId }
            Contacts.create(ownerId, contactId)
            awaitBrokering()
            val (ownerSubscriber, contactSubscriber, userSubscriber) = setOf(ownerId, contactId, userId)
                .map { accountsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            Contacts.deleteUserEntries(contactId)
            awaitBrokering()
            val actual = ownerSubscriber.values().map { (it as DeletedContact).getUserId() }
            assertEquals(listOf(contactId), actual)
            setOf(contactSubscriber, userSubscriber).forEach { it.assertNoValues() }
        }
    }
}
