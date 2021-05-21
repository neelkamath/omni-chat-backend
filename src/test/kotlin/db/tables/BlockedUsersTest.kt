package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.BlockedAccount
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UnblockedAccount
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.toLinkedHashSet
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.*

@ExtendWith(DbExtension::class)
class BlockedUsersTest {
    @Nested
    inner class Create {
        @Test
        fun `The user must be blocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.userId }
            BlockedUsers.create(blockerId, blockedId)
            assertEquals(blockedId, BlockedUsers.readBlockedUserIdList(blockerId).first())
        }

        @Test
        fun `The user mustn't be able to block themselves`() {
            val userId = createVerifiedUsers(1).first().userId
            BlockedUsers.create(userId, userId)
            assertEquals(0, BlockedUsers.count())
        }

        @Test
        fun `Blocking the user twice mustn't create two entries`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.userId }
            repeat(2) { BlockedUsers.create(blockerId, blockedId) }
            assertEquals(1, BlockedUsers.count())
        }

        @Test
        fun `Blocking the user must notify only the blocker`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.userId }
            val (blockerSubscriber, blockedSubscriber) =
                listOf(blockerId, blockedId).map { accountsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            BlockedUsers.create(blockerId, blockedId)
            awaitBrokering()
            val actual = blockerSubscriber.values().map { (it as BlockedAccount).id }
            assertEquals(listOf(blockedId), actual)
            blockedSubscriber.assertNoValues()
        }

        @Test
        fun `The blocker mustn't be notified when they attempt to block a user who's already blocked`(): Unit =
            runBlocking {
                val (blockerId, blockedId) = createVerifiedUsers(2).map { it.userId }
                BlockedUsers.create(blockerId, blockedId)
                awaitBrokering()
                val subscriber = accountsNotifier.subscribe(blockerId).subscribeWith(TestSubscriber())
                BlockedUsers.create(blockerId, blockedId)
                awaitBrokering()
                subscriber.assertNoValues()
            }
    }

    @Nested
    inner class ReadBlockedUserIdList {
        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            val actual = BlockedUsers.readBlockedUserIdList(blockerUserId).toLinkedHashSet()
            assertEquals(blockedUserIdList, actual)
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = blockedUserIdList.elementAt(index))
            val actual = BlockedUsers.readBlockedUserIdList(blockerUserId, pagination).toLinkedHashSet()
            assertEquals(blockedUserIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            val first = 3
            val actual = BlockedUsers.readBlockedUserIdList(blockerUserId, ForwardPagination(first))
            assertEquals(blockedUserIdList.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            val index = 4
            val pagination = ForwardPagination(after = blockedUserIdList.elementAt(index))
            val actual = BlockedUsers.readBlockedUserIdList(blockerUserId, pagination)
            assertEquals(blockedUserIdList.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val (adminId, blockedUserIdList) = createBlockedUsers()
            val pagination = ForwardPagination(after = blockedUserIdList.last())
            assertEquals(0, BlockedUsers.readBlockedUserIdList(adminId, pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            BlockedUsers.delete(blockerUserId, blockedUserIdList.elementAt(3))
            val first = 3
            val index = 1
            val pagination = ForwardPagination(first, after = blockedUserIdList.elementAt(index))
            val expected = listOf(2, 4, 5).map(blockedUserIdList::elementAt).toLinkedHashSet()
            val actual = BlockedUsers.readBlockedUserIdList(blockerUserId, pagination)
            assertEquals(expected, actual)
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            val index = 4
            val cursor = blockedUserIdList.elementAt(index)
            BlockedUsers.delete(blockerUserId, blockedUserId = cursor)
            val actual = BlockedUsers.readBlockedUserIdList(blockerUserId, ForwardPagination(after = cursor))
            assertEquals(blockedUserIdList.drop(index + 1).toLinkedHashSet(), actual)
        }
    }

    @Nested
    inner class ReadCursor {
        @Test
        fun `If there are zero items, the start and end cursors must be 'null'`() {
            val blockerId = createVerifiedUsers(1).first().userId
            assertNull(BlockedUsers.readCursor(blockerId, CursorType.START))
            assertNull(BlockedUsers.readCursor(blockerId, CursorType.END))
        }

        @Test
        fun `The start and end cursors must point to the first and last items respectively`() {
            val (blockerUserId, blockedUserIdList) = createBlockedUsers()
            assertEquals(blockedUserIdList.first(), BlockedUsers.readCursor(blockerUserId, CursorType.START))
            assertEquals(blockedUserIdList.last(), BlockedUsers.readCursor(blockerUserId, CursorType.END))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `The user must be notified of the unblocked user`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.userId }
            BlockedUsers.create(blockerId, blockedId)
            awaitBrokering()
            val (blockerSubscriber, blockedSubscriber) =
                listOf(blockerId, blockedId).map { accountsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            assertTrue(BlockedUsers.delete(blockerId, blockedId))
            awaitBrokering()
            val actual = blockerSubscriber.values().map { (it as UnblockedAccount).getUserId() }
            assertEquals(listOf(blockedId), actual)
            blockedSubscriber.assertNoValues()
        }

        @Test
        fun `The user mustn't be notified when unblocking a user who wasn't blocked`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.userId }
            val subscriber = accountsNotifier.subscribe(blockerId).subscribeWith(TestSubscriber())
            assertFalse(BlockedUsers.delete(blockerId, blockedId))
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    private data class CreatedBlockedUsers(val blockerUserId: Int, val blockedUserIdList: LinkedHashSet<Int>)

    private fun createBlockedUsers(blockedUserIdListCount: Int = 10): CreatedBlockedUsers {
        val blockerUserId = createVerifiedUsers(1).first().userId
        createVerifiedUsers(blockedUserIdListCount)
            .forEach { BlockedUsers.create(blockerUserId, blockedUserId = it.userId) }
        return CreatedBlockedUsers(blockerUserId, BlockedUsers.readBlockedUserIdList(blockerUserId))
    }
}
