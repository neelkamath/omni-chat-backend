package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.deleteUser
import com.neelkamath.omniChat.graphql.routing.AccountEdge
import com.neelkamath.omniChat.graphql.routing.AccountsConnection
import com.neelkamath.omniChat.graphql.routing.ExitedUser
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

class PicTest {
    @Nested
    inner class Init {
        @Test
        fun `Passing an excessively large image should cause an exception to be thrown`() {
            assertFailsWith<IllegalArgumentException> { Pic(ByteArray(Pic.MAX_BYTES + 1), Pic.Type.PNG) }
        }
    }
}

@ExtendWith(DbExtension::class)
class DbTest {
    @Nested
    inner class DeleteUserFromDb {
        @Test
        fun `An exception should be thrown when the admin of a nonempty group chat deletes their data`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertFailsWith<IllegalArgumentException> { deleteUserFromDb(adminId) }
        }

        @Test
        fun `The deleted user should be unsubscribed via the new group chats broker`() {
            runBlocking {
                val userId = createVerifiedUsers(1)[0].info.id
                val subscriber =
                    newGroupChatsNotifier.safelySubscribe(NewGroupChatsAsset(userId)).subscribeWith(TestSubscriber())
                deleteUserFromDb(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `A private chat should be deleted for the other other if the user deleted it before deleting their data`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            deleteUserFromDb(user1Id)
            assertEquals(0, PrivateChats.count())
        }

        @Test
        fun `The deleted user should be unsubscribed from contact updates`() {
            runBlocking {
                val userId = createVerifiedUsers(1)[0].info.id
                val subscriber =
                    contactsNotifier.safelySubscribe(ContactsAsset(userId)).subscribeWith(TestSubscriber())
                deleteUserFromDb(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `Only the deleted subscriber should be unsubscribed from updated chats`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), listOf(userId))
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                    .map { updatedChatsNotifier.safelySubscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
                deleteUserFromDb(userId)
                awaitBrokering()
                adminSubscriber.assertValue(ExitedUser(userId, chatId))
                userSubscriber.assertComplete()
            }
        }

        @Test
        fun `The user should be unsubscribed from message updates`() {
            runBlocking {
                val userId = createVerifiedUsers(1)[0].info.id
                val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(userId)).subscribeWith(TestSubscriber())
                deleteUserFromDb(userId)
                subscriber.assertComplete()
            }
        }
    }

    @Nested
    @Suppress("ClassName")
    inner class AccountsConnection_build {
        /** Creates [count] users. */
        private fun createAccountEdges(count: Int = 3): List<AccountEdge> =
            createVerifiedUsers(count).zip(Users.read()).map { (user, cursor) -> AccountEdge(user.info, cursor) }

        @Test
        fun `Every user should be retrieved if neither cursor nor limit get supplied`() {
            val edges = createAccountEdges()
            assertEquals(edges, AccountsConnection.build(edges).edges)
        }

        @Test
        fun `Using a deleted user's cursor should cause pagination to work as if the user still exists`() {
            val edges = createAccountEdges(10)
            val index = 5
            val deletedUser = edges[index]
            deleteUser(deletedUser.node.id)
            val first = 3
            assertEquals(
                edges.subList(index + 1, index + 1 + first),
                AccountsConnection.build(edges, ForwardPagination(first, deletedUser.cursor)).edges
            )
        }

        @Test
        fun `When retrieving the first of many users, the page info should state that there are only users after`() {
            AccountsConnection.build(createAccountEdges(), ForwardPagination(first = 1)).pageInfo.run {
                assertTrue(hasNextPage)
                assertFalse(hasPreviousPage)
            }
        }

        @Test
        fun `Retrieving the last user should cause the page info to state there are only users before`() {
            val edges = createAccountEdges()
            AccountsConnection.build(edges, ForwardPagination(after = edges[1].cursor)).pageInfo.run {
                assertFalse(hasNextPage)
                assertTrue(hasPreviousPage)
            }
        }

        @Test
        fun `The start and end cursors should be null if there are no users`() {
            AccountsConnection.build(accountEdges = listOf()).pageInfo.run {
                assertNull(startCursor)
                assertNull(endCursor)
            }
        }

        @Test
        fun `The start and end cursors should be the same if there's only one user`() {
            AccountsConnection.build(createAccountEdges(1)).pageInfo.run { assertEquals(endCursor, startCursor) }
        }

        @Test
        fun `The first and last cursors should be the first and last users respectively`() {
            val edges = createAccountEdges()
            AccountsConnection.build(edges).pageInfo.run {
                assertEquals(edges[0].cursor, startCursor)
                assertEquals(edges.last().cursor, endCursor)
            }
        }

        @Test
        fun `Only the number of users specified by the limit should be returned from after the cursor`() {
            val edges = createAccountEdges(10)
            val first = 3
            val index = 5
            assertEquals(
                edges.subList(index + 1, index + 1 + first),
                AccountsConnection.build(edges, ForwardPagination(first, edges[index].cursor)).edges
            )
        }

        @Test
        fun `The "limit" of users should be retrieved from the first user when there's no cursor`() {
            val edges = createAccountEdges(5)
            val first = 3
            assertEquals(edges.subList(0, first), AccountsConnection.build(edges, ForwardPagination(first)).edges)
        }

        @Test
        fun `Every user after the cursor should be retrieved when there's no limit`() {
            val edges = createAccountEdges(10)
            val index = 5
            assertEquals(
                edges.drop(index + 1),
                AccountsConnection.build(edges, ForwardPagination(after = edges[index].cursor)).edges
            )
        }

        @Test
        fun `Supplying unsorted rows shouldn't affect pagination`() {
            val edges = createVerifiedUsers(10)
                .zip(Users.read())
                .map { (user, cursor) -> AccountEdge(user.info, cursor) }
            val first = 3
            val index = 5
            assertEquals(
                edges.subList(index + 1, index + 1 + first),
                AccountsConnection.build(edges.shuffled(), ForwardPagination(first, edges[index].cursor)).edges
            )
        }
    }
}