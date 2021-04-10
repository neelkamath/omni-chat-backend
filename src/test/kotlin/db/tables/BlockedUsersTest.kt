package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.accountsNotifier
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.graphql.routing.BlockedAccount
import com.neelkamath.omniChatBackend.graphql.routing.PageInfo
import com.neelkamath.omniChatBackend.graphql.routing.UnblockedAccount
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.toLinkedHashSet
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.collections.LinkedHashSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.drop
import kotlin.collections.elementAt
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.last
import kotlin.collections.lastOrNull
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.take
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class BlockedUsersTest {
    @Nested
    inner class Search {
        @Test
        fun `Only blocked users must be searched`() {
            // Create three users instead of two to verify that the user who isn't blocked doesn't get searched.
            val (blockerId, blockedId) = createVerifiedUsers(3).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            val actual = BlockedUsers.search(blockerId, query = "").edges.map { it.node.id }
            assertEquals(listOf(blockedId), actual)
        }

        @Test
        fun `Only matching users must be found`() {
            val (blocker, blocked1, blocked2) = createVerifiedUsers(3).map { it.info }
            listOf(blocked1, blocked2).forEach { BlockedUsers.create(blocker.id, it.id) }
            val actual = BlockedUsers.search(blocker.id, query = blocked1.username.value).edges.map { it.node.id }
            assertEquals(listOf(blocked1.id), actual)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `The user must be blocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            assertEquals(blockedId, BlockedUsers.readBlockedUsers(blockerId).first().blockedUserId)
        }

        @Test
        fun `The user mustn't be able to block themselves`() {
            val userId = createVerifiedUsers(1).first().info.id
            BlockedUsers.create(userId, userId)
            assertEquals(0, BlockedUsers.count())
        }

        @Test
        fun `Blocking the user twice mustn't create two entries`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            repeat(2) { BlockedUsers.create(blockerId, blockedId) }
            assertEquals(1, BlockedUsers.count())
        }

        @Test
        fun `Blocking the user must notify only the blocker`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            val (blockerSubscriber, blockedSubscriber) =
                listOf(blockerId, blockedId).map { accountsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            BlockedUsers.create(blockerId, blockedId)
            awaitBrokering()
            blockerSubscriber.assertValue(BlockedAccount.build(blockedId))
            blockedSubscriber.assertNoValues()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `The user must be notified of the unblocked user`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            awaitBrokering()
            val (blockerSubscriber, blockedSubscriber) =
                listOf(blockerId, blockedId).map { accountsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            assertTrue(BlockedUsers.delete(blockerId, blockedId))
            awaitBrokering()
            blockerSubscriber.assertValue(UnblockedAccount(blockedId))
            blockedSubscriber.assertNoValues()
        }

        @Test
        fun `The user mustn't be notified when unblocking a user who wasn't blocked`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            val subscriber = accountsNotifier.subscribe(blockerId).subscribeWith(TestSubscriber())
            assertFalse(BlockedUsers.delete(blockerId, blockedId))
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    @Nested
    inner class DeleteUser {
        @Test
        fun `Entries with the user must be deleted`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            BlockedUsers.create(user1Id, user2Id)
            listOf(user1Id, user3Id).forEach { BlockedUsers.create(user2Id, it) }
            BlockedUsers.deleteUser(user1Id)
            assertEquals(0, BlockedUsers.readBlockedUsers(user1Id).size)
            assertEquals(listOf(user3Id), BlockedUsers.readBlockedUsers(user2Id).map { it.blockedUserId })
        }
    }

    private data class CreatedBlockedUsers(
        val blockerUserId: Int,
        val blockedUsers: LinkedHashSet<BlockedUsers.BlockedUserCursor>,
    )

    private fun createBlockedUsers(blockedUsersCount: Int = 10): CreatedBlockedUsers {
        val blockerUserId = createVerifiedUsers(1).first().info.id
        createVerifiedUsers(blockedUsersCount)
            .forEach { BlockedUsers.create(blockerUserId, blockedUserId = it.info.id) }
        return CreatedBlockedUsers(blockerUserId, BlockedUsers.readBlockedUsers(blockerUserId))
    }

    @Nested
    inner class ReadBlockedUsers {
        @Test
        fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {
            val (adminId, blockedUsers) = createBlockedUsers()
            val pagination = ForwardPagination(after = blockedUsers.last().cursor)
            val paginatedMessages = BlockedUsers.readBlockedUsers(adminId, pagination)
            val (hasNextPage, hasPreviousPage) =
                BlockedUsers.readPageInfo(adminId, paginatedMessages.lastOrNull()?.cursor, pagination)
            assertEquals(0, paginatedMessages.size)
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers()
            val actual = BlockedUsers.readBlockedUsers(blockerUserId).toLinkedHashSet()
            assertEquals(blockedUsers, actual)
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = blockedUsers.elementAt(index).cursor)
            val actual = BlockedUsers.readBlockedUsers(blockerUserId, pagination).toLinkedHashSet()
            assertEquals(blockedUsers.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers()
            val first = 3
            val actual = BlockedUsers.readBlockedUsers(blockerUserId, ForwardPagination(first))
            assertEquals(blockedUsers.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers()
            val index = 4
            val pagination = ForwardPagination(after = blockedUsers.elementAt(index).cursor)
            val actual = BlockedUsers.readBlockedUsers(blockerUserId, pagination)
            assertEquals(blockedUsers.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers()
            BlockedUsers.delete(blockerUserId, blockedUsers.elementAt(3).blockedUserId)
            val first = 3
            val index = 1
            val pagination = ForwardPagination(first, after = blockedUsers.elementAt(index).cursor)
            val expected = listOf(2, 4, 5).map(blockedUsers::elementAt).toLinkedHashSet()
            val actual = BlockedUsers.readBlockedUsers(blockerUserId, pagination)
            assertEquals(expected, actual)
        }
    }

    @Nested
    inner class ReadPageInfo {
        @Test
        fun `If there are zero items, the page info must indicate such`() {
            val (blockerUserId) = createBlockedUsers(blockedUsersCount = 0)
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            assertEquals(expected, BlockedUsers.readPageInfo(blockerUserId, lastEdgeCursor = null))
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers(blockedUsersCount = 1)
            val lastEdgeCursor = blockedUsers.first().cursor
            val expected = PageInfo(
                hasNextPage = false,
                hasPreviousPage = false,
                startCursor = lastEdgeCursor,
                endCursor = lastEdgeCursor,
            )
            assertEquals(expected, BlockedUsers.readPageInfo(blockerUserId, lastEdgeCursor))
        }

        @Test
        fun `The first and last cursors must be the first and last items respectively`() {
            val (blockerUserId, blockedUsers) = createBlockedUsers()
            val (_, _, startCursor, endCursor) = BlockedUsers.readPageInfo(blockerUserId, lastEdgeCursor = null)
            assertEquals(blockedUsers.first().cursor, startCursor)
            assertEquals(blockedUsers.last().cursor, endCursor)
        }
    }
}
