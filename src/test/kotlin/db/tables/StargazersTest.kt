package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.routing.PageInfo
import com.neelkamath.omniChatBackend.graphql.routing.UnstarredChat
import com.neelkamath.omniChatBackend.linkedHashSetOf
import com.neelkamath.omniChatBackend.toLinkedHashSet
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun createStarredChat(messages: Int = 10): StarredChat {
        val adminId = createVerifiedUsers(1).first().info.id
        val chatId = GroupChats.create(listOf(adminId))
        val messageIdList = (1..messages).map {
            Messages.message(adminId, chatId).also { Stargazers.create(adminId, it) }
        }
        return StarredChat(adminId, messageIdList)
    }

    @Nested
    inner class ReadMessageIdList {
        @Test
        fun `Only the user's messages must be retrieved`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (messageId) = listOf(user1Id, user2Id).map { userId ->
                Messages.message(userId, chatId).also { Stargazers.create(userId, it) }
            }
            assertEquals(linkedHashSetOf(messageId), Stargazers.readMessageIdList(user1Id))
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (adminId, messageIdList) = createStarredChat()
            assertEquals(messageIdList.toLinkedHashSet(), Stargazers.readMessageIdList(adminId))
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (adminId, messageIdList) = createStarredChat()
            val index = 5
            val messageId = messageIdList[index]
            Messages.delete(messageId)
            val actual = Stargazers.readMessageIdList(adminId, ForwardPagination(after = messageId))
            assertEquals(messageIdList.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, messageIdList) = createStarredChat()
            val first = 3
            val index = 5
            val expected = messageIdList.slice(index + 1..index + first).toLinkedHashSet()
            val pagination = ForwardPagination(first, after = messageIdList[index])
            val actual = Stargazers.readMessageIdList(adminId, pagination)
            assertEquals(expected, actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, messageIdList) = createStarredChat()
            val first = 3
            val actual = Stargazers.readMessageIdList(adminId, ForwardPagination(first))
            assertEquals(messageIdList.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `When requesting items after the start cursor, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(after = messageIdList[0])
            val lastMessageId = Stargazers.readMessageIdList(adminId, pagination).lastOrNull()
            val (hasNextPage, hasPreviousPage) = Stargazers.readPageInfo(adminId, lastMessageId, pagination)
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, messageIdList) = createStarredChat()
            val index = 5
            val pagination = ForwardPagination(after = messageIdList[index])
            val expected = messageIdList.drop(index + 1).toLinkedHashSet()
            val actual = Stargazers.readMessageIdList(adminId, pagination)
            assertEquals(expected, actual)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (adminId, messageIdList) = createStarredChat()
            Messages.delete(messageIdList[3])
            val paginatedMessages =
                Stargazers.readMessageIdList(adminId, ForwardPagination(first = 3, after = messageIdList[1]))
            assertEquals(linkedHashSetOf(messageIdList[2], messageIdList[4], messageIdList[5]), paginatedMessages)
        }
    }

    @Nested
    inner class ReadPageInfo {
        @Test
        fun `When requesting zero items sans cursor, the page info must indicate such`() {
            val (adminId) = createStarredChat()
            val pagination = ForwardPagination(first = 0)
            val lastMessageId = Stargazers.readMessageIdList(adminId, pagination).lastOrNull()
            val (hasNextPage, hasPreviousPage) = Stargazers.readPageInfo(adminId, lastMessageId, pagination)
            assertTrue(hasNextPage)
            assertFalse(hasPreviousPage)
        }

        @Test
        fun `When requesting zero items after the end cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(first = 0, after = messageIdList.last())
            val lastMessageId = Stargazers.readMessageIdList(adminId, pagination).lastOrNull()
            val (hasNextPage, hasPreviousPage) = Stargazers.readPageInfo(adminId, lastMessageId, pagination)
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Given items 1-10, when requesting zero items after item 5, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(first = 0, after = messageIdList[4])
            val lastMessageId = Stargazers.readMessageIdList(adminId, pagination).lastOrNull()
            val (hasNextPage, hasPreviousPage) = Stargazers.readPageInfo(adminId, lastMessageId, pagination)
            assertTrue(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(after = messageIdList.last())
            val paginatedMessages = Stargazers.readMessageIdList(adminId, pagination)
            val (hasNextPage, hasPreviousPage) =
                Stargazers.readPageInfo(adminId, paginatedMessages.lastOrNull(), pagination)
            assertEquals(0, paginatedMessages.size)
            assertEquals(false, hasNextPage)
            assertEquals(true, hasPreviousPage)
        }

        @Test
        fun `Retrieving the first of many items must cause the page info to state there are only items after it`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(first = 1, after = messageIdList[0])
            val lastMessageId = Stargazers.readMessageIdList(adminId, pagination).lastOrNull()
            Stargazers.readPageInfo(adminId, lastMessageId, pagination).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Retrieving the last of many items must cause the page info to state there are only items before it`() {
            val (adminId, messageIdList) = createStarredChat()
            val pagination = ForwardPagination(after = messageIdList[messageIdList.size - 2])
            val lastMessageId = Stargazers.readMessageIdList(adminId, pagination).lastOrNull()
            Stargazers.readPageInfo(adminId, lastMessageId, pagination).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `If there are zero items, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            assertEquals(expected, Stargazers.readPageInfo(adminId, lastMessageId = null))
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
            val actual = Stargazers.readPageInfo(adminId, lastMessageId = messageIdList[0])
            assertEquals(expected, actual)
        }

        @Test
        fun `The first and last cursors must be the first and last items respectively`() {
            val (adminId, messageIdList) = createStarredChat()
            val (_, _, startCursor, endCursor) = Stargazers.readPageInfo(adminId, lastMessageId = null)
            assertEquals(messageIdList[0], startCursor)
            assertEquals(messageIdList.last(), endCursor)
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
            assertTrue(Stargazers.readMessageIdList(userId).isEmpty())
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
