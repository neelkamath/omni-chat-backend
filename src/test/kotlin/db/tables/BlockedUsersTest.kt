package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.accountsNotifier
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.BlockedAccount
import com.neelkamath.omniChat.graphql.routing.UnblockedAccount
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class BlockedUsersTest {
    @Nested
    inner class Create {
        @Test
        fun `The user must be blocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            assertEquals(blockedId, BlockedUsers.read(blockerId).edges[0].node.id)
        }

        @Test
        fun `The user mustn't be able to block themselves`() {
            val userId = createVerifiedUsers(1)[0].info.id
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
                listOf(blockerId, blockedId).map { accountsNotifier.safelySubscribe(it) }
            BlockedUsers.create(blockerId, blockedId)
            awaitBrokering()
            blockerSubscriber.assertValue(BlockedAccount.build(blockedId))
            blockedSubscriber.assertNoValues()
        }
    }

    @Nested
    inner class Read {
        @Test
        fun `Blocked users must be read`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            listOf(user2Id, user3Id).forEach { BlockedUsers.create(user1Id, it) }
            BlockedUsers.create(user2Id, user1Id)
            BlockedUsers.create(user3Id, user2Id)
            val users = BlockedUsers.read(user1Id).edges.map { it.node.id }
            assertEquals(listOf(user2Id, user3Id), users)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `The user must be unblocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            BlockedUsers.delete(blockerId, blockedId)
            assertEquals(0, BlockedUsers.count())
        }

        @Test
        fun `The user must be notified of the unblocked user`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            val (blockerSubscriber, blockedSubscriber) =
                listOf(blockerId, blockedId).map { accountsNotifier.safelySubscribe(it) }
            BlockedUsers.delete(blockerId, blockedId)
            awaitBrokering()
            blockerSubscriber.assertValue(UnblockedAccount(blockedId))
            blockedSubscriber.assertNoValues()
        }

        @Test
        fun `The user mustn't be notified when unblocking a user who wasn't blocked`(): Unit = runBlocking {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            val subscriber = accountsNotifier.safelySubscribe(blockerId)
            BlockedUsers.delete(blockerId, blockedId)
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
            assertEquals(0, BlockedUsers.read(user1Id).edges.size)
            val users = BlockedUsers.read(user2Id).edges.map { it.node.id }
            assertEquals(listOf(user3Id), users)
        }
    }
}
