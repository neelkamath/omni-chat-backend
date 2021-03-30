package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.graphql.routing.PageInfo
import com.neelkamath.omniChat.graphql.routing.UnstarredChat
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class StargazersTest {
    @Nested
    inner class Create {
        @Test
        fun `Starring must only notify the stargazer`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                awaitBrokering()
                val (user1Subscriber, user2Subscriber) =
                    listOf(user1Id, user2Id).map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                Stargazers.create(user1Id, messageId)
                awaitBrokering()
                user1Subscriber.assertValue(Messages.readMessage(user1Id, messageId).toUpdatedMessage())
                user2Subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class DeleteUserStar {
        @Test
        fun `Deleting a star must only notify the deleter`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                Stargazers.create(user1Id, messageId)
                awaitBrokering()
                val (user1Subscriber, user2Subscriber) =
                    listOf(user1Id, user2Id).map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                Stargazers.deleteUserStar(user1Id, messageId)
                awaitBrokering()
                user1Subscriber.assertValue(Messages.readMessage(user1Id, messageId).toUpdatedMessage())
                user2Subscriber.assertNoValues()
            }
        }

        @Test
        fun `Deleting a nonexistent star mustn't cause anything to happen`() {
            runBlocking {
                val adminId = createVerifiedUsers(1).first().info.id
                val chatId = GroupChats.create(listOf(adminId))
                val messageId = Messages.message(adminId, chatId)
                awaitBrokering()
                val subscriber = messagesNotifier.subscribe(adminId).subscribeWith(TestSubscriber())
                Stargazers.deleteUserStar(adminId, messageId)
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }
    }

    private data class StarredChat(val adminId: Int, val messageIdList: List<Int>)

    @Nested
    inner class Read {
        @Test
        fun `Only the user's messages must be retrieved`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (messageId) = listOf(user1Id, user2Id).map { userId ->
                Messages.message(userId, chatId).also { Stargazers.create(userId, it) }
            }
            assertEquals(listOf(messageId), Stargazers.read(user1Id).edges.map { it.node.messageId })
        }

        private fun createStarredChat(messages: Int = 10): StarredChat {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..messages).map {
                Messages.message(adminId, chatId).also { Stargazers.create(adminId, it) }
            }
            return StarredChat(adminId, messageIdList)
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (adminId, messageIdList) = createStarredChat()
            assertEquals(messageIdList, Stargazers.read(adminId).edges.map { it.node.messageId })
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (adminId, messageIdList) = createStarredChat()
            val index = 5
            val messageId = messageIdList[index]
            Messages.delete(messageId)
            val actual = Stargazers.read(adminId, ForwardPagination(after = messageId)).edges.map { it.node.messageId }
            assertEquals(messageIdList.drop(index + 1), actual)
        }

        @Test
        fun `Retrieving the first of many items must cause the page info to state there are only items after it`() {
            val (adminId, messageIdList) = createStarredChat()
            val pageInfo = Stargazers.read(adminId, ForwardPagination(first = 1, after = messageIdList[0])).pageInfo
            assertTrue(pageInfo.hasNextPage)
        }

        @Test
        fun `Retrieving the last of many items must cause the page info to state there are only items before it`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(after = messageIdList[messageIdList.size - 2])
            Stargazers.read(adminId, pagination).pageInfo.hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `If there are no items, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            assertEquals(expected, Stargazers.read(adminId).pageInfo)
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val (adminId, messageIdList) = createStarredChat(messages = 1)
            val expected = PageInfo(
                hasNextPage = false,
                hasPreviousPage = false,
                startCursor = messageIdList[0],
                endCursor = messageIdList[0],
            )
            assertEquals(expected, Stargazers.read(adminId).pageInfo)
        }

        @Test
        fun `The first and last cursors must be the first and last items respectively`() {
            val (adminId, messageIdList) = createStarredChat()
            val (_, _, startCursor, endCursor) = Stargazers.read(adminId).pageInfo
            assertEquals(messageIdList[0], startCursor)
            assertEquals(messageIdList.last(), endCursor)
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, messageIdList) = createStarredChat()
            val first = 3
            val index = 5
            val expected = messageIdList.slice(index + 1..index + first)
            val pagination = ForwardPagination(first, after = messageIdList[index])
            val actual = Stargazers.read(adminId, pagination).edges.map { it.node.messageId }
            assertEquals(expected, actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, messageIdList) = createStarredChat()
            val first = 3
            val actual = Stargazers.read(adminId, ForwardPagination(first)).edges.map { it.node.messageId }
            assertEquals(messageIdList.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, messageIdList) = createStarredChat()
            val index = 5
            val pagination = ForwardPagination(after = messageIdList[index])
            val actual = Stargazers.read(adminId, pagination).edges.map { it.node.messageId }
            assertEquals(messageIdList.drop(index + 1), actual)
        }
    }

    @Nested
    inner class DeleteUserChat {
        @Test
        fun `Every message the user starred in the chat must be unstarred`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(userId, messageId)
            GroupChatUsers.removeUsers(chatId, userId)
            assertTrue(Stargazers.read(userId).edges.isEmpty())
        }

        @Test
        fun `Only the user must be notified of the unstarred messages`(): Unit = runBlocking {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(user1Id, messageId)
            awaitBrokering()
            val (user1Subscriber, user2Subscriber) =
                listOf(user1Id, user2Id).map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            GroupChatUsers.removeUsers(chatId, user1Id)
            awaitBrokering()
            user1Subscriber.assertValue(UnstarredChat(chatId))
            user2Subscriber.assertNoValues()
        }
    }

    @Nested
    inner class DeleteStar {
        @Test
        fun `Deleting a message's stars must only notify its stargazers`() {
            runBlocking {
                val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id))
                val messageId = Messages.message(adminId, chatId)
                listOf(adminId, user1Id).forEach { Stargazers.create(it, messageId) }
                awaitBrokering()
                val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                    .map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                Stargazers.deleteStar(messageId)
                awaitBrokering()
                mapOf(adminId to adminSubscriber, user1Id to user1Subscriber).forEach { (userId, subscriber) ->
                    subscriber.assertValue(Messages.readMessage(userId, messageId).toUpdatedMessage())
                }
                user2Subscriber.assertNoValues()
            }
        }
    }
}
