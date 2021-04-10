package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.routing.PageInfo
import com.neelkamath.omniChatBackend.graphql.routing.UnstarredChat
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

    private data class StarredChat(val adminId: Int, val messageCursors: LinkedHashSet<Stargazers.MessageCursor>)

    private fun createStarredChat(messages: Int = 10): StarredChat {
        val adminId = createVerifiedUsers(1).first().info.id
        val chatId = GroupChats.create(listOf(adminId))
        repeat(messages) {
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
        }
        return StarredChat(adminId, Stargazers.readMessageCursors(adminId))
    }

    @Nested
    inner class ReadMessageCursors {
        @Test
        fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {
            val (adminId, messageCursors) = createStarredChat()
            val pagination = ForwardPagination(after = messageCursors.last().cursor)
            val paginatedMessages = Stargazers.readMessageCursors(adminId, pagination)
            val (hasNextPage, hasPreviousPage) =
                Stargazers.readPageInfo(adminId, paginatedMessages.lastOrNull()?.cursor, pagination)
            assertEquals(0, paginatedMessages.size)
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Only the user's messages must be retrieved`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (messageId) = listOf(user1Id, user2Id).map { userId ->
                Messages.message(userId, chatId).also { Stargazers.create(userId, it) }
            }
            val actual = Stargazers.readMessageCursors(user1Id).map { it.messageId }.toLinkedHashSet()
            assertEquals(linkedHashSetOf(messageId), actual)
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (adminId, messageCursors) = createStarredChat()
            assertEquals(messageCursors, Stargazers.readMessageCursors(adminId))
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, messageCursors) = createStarredChat()
            val first = 3
            val index = 5
            val expected = messageCursors.slice(index + 1..index + first)
            val pagination = ForwardPagination(first, after = messageCursors.elementAt(index).cursor)
            assertEquals(expected, Stargazers.readMessageCursors(adminId, pagination))
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, messageCursors) = createStarredChat()
            val first = 3
            val actual = Stargazers.readMessageCursors(adminId, ForwardPagination(first))
            assertEquals(messageCursors.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, messageCursors) = createStarredChat()
            val index = 5
            val pagination = ForwardPagination(after = messageCursors.elementAt(index).cursor)
            val actual = Stargazers.readMessageCursors(adminId, pagination)
            assertEquals(messageCursors.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (adminId, messageCursors) = createStarredChat()
            Messages.delete(messageCursors.elementAt(3).messageId)
            val expected = listOf(2, 4, 5).map(messageCursors::elementAt).toLinkedHashSet()
            val pagination = ForwardPagination(first = 3, after = messageCursors.elementAt(1).cursor)
            val actual = Stargazers.readMessageCursors(adminId, pagination)
            assertEquals(expected, actual)
        }
    }

    @Nested
    inner class ReadPageInfo {
        @Test
        fun `If there are zero items, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            assertEquals(expected, Stargazers.readPageInfo(adminId, lastEdgeCursor = null))
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val (adminId, messageCursors) = createStarredChat(messages = 1)
            val lastEdgeCursor = messageCursors.first().cursor
            val expected = PageInfo(
                hasNextPage = false,
                hasPreviousPage = false,
                startCursor = lastEdgeCursor,
                endCursor = lastEdgeCursor,
            )
            assertEquals(expected, Stargazers.readPageInfo(adminId, lastEdgeCursor))
        }

        @Test
        fun `The first and last cursors must be the first and last items respectively`() {
            val (adminId, messageCursors) = createStarredChat()
            val (_, _, startCursor, endCursor) = Stargazers.readPageInfo(adminId, lastEdgeCursor = null)
            assertEquals(messageCursors.first().cursor, startCursor)
            assertEquals(messageCursors.last().cursor, endCursor)
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
            assertTrue(Stargazers.readMessageCursors(userId).isEmpty())
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
