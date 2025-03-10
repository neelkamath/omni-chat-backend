package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UnbookmarkedChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedMessage
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.toLinkedHashSet
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class BookmarksTest {
    @Nested
    inner class Create {
        @Test
        fun `Creating a bookmark must only notify the user who bookmarked it`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                awaitBrokering()
                val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                    .map { messagesNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
                Bookmarks.create(user1Id, messageId)
                awaitBrokering()
                val actual = user1Subscriber.values().map { (it as UpdatedMessage).getMessageId() }
                assertEquals(listOf(messageId), actual)
                user2Subscriber.assertNoValues()
            }
        }
    }

    /** The [adminId] of a group chat containing the [messageIdList] (sorted in ascending order). */
    private data class BookmarkedChat(val adminId: Int, val messageIdList: LinkedHashSet<Int>)

    /** Creates the number of [messages] in the [BookmarkedChat.messageIdList]. */
    private fun createBookmarkedChat(messages: Int = 10): BookmarkedChat {
        val adminId = createVerifiedUsers(1).first().userId
        val chatId = GroupChats.create(setOf(adminId))
        repeat(messages) {
            val messageId = Messages.message(adminId, chatId)
            Bookmarks.create(adminId, messageId)
        }
        return BookmarkedChat(adminId, Bookmarks.readMessageIdList(adminId))
    }

    @Nested
    inner class ReadMessageIdList {
        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            assertEquals(messageIdList, Bookmarks.readMessageIdList(adminId))
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = messageIdList.elementAt(index))
            val actual = Bookmarks.readMessageIdList(adminId, pagination)
            assertEquals(messageIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            val first = 3
            val actual = Bookmarks.readMessageIdList(adminId, ForwardPagination(first))
            assertEquals(messageIdList.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            val index = 4
            val pagination = ForwardPagination(after = messageIdList.elementAt(index))
            val actual = Bookmarks.readMessageIdList(adminId, pagination)
            assertEquals(messageIdList.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            val pagination = ForwardPagination(after = messageIdList.last())
            assertEquals(0, Bookmarks.readMessageIdList(adminId, pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            Messages.delete(messageIdList.elementAt(3))
            val expected = listOf(2, 4, 5).map(messageIdList::elementAt).toLinkedHashSet()
            val pagination = ForwardPagination(first = 3, after = messageIdList.elementAt(1))
            assertEquals(expected, Bookmarks.readMessageIdList(adminId, pagination))
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            val index = 3
            val messageId = messageIdList.elementAt(index)
            Messages.delete(messageId)
            val actual = Bookmarks.readMessageIdList(adminId, ForwardPagination(after = messageId))
            assertEquals(messageIdList.drop(index + 1).toLinkedHashSet(), actual)
        }
    }

    @Nested
    inner class ReadCursor {
        @Test
        fun `If there are zero items, the start and end cursors must be 'null'`() {
            val (adminId) = createBookmarkedChat(messages = 0)
            assertNull(Bookmarks.readCursor(adminId, CursorType.START))
            assertNull(Bookmarks.readCursor(adminId, CursorType.END))
        }

        @Test
        fun `If there's one item, the start and end cursors must be the only item`() {
            val (adminId, messageIdList) = createBookmarkedChat(messages = 1)
            val messageId = messageIdList.first()
            assertEquals(messageId, Bookmarks.readCursor(adminId, CursorType.START))
            assertEquals(messageId, Bookmarks.readCursor(adminId, CursorType.END))
        }

        @Test
        fun `The start and end cursors must point to the first and last items respectively`() {
            val (adminId, messageIdList) = createBookmarkedChat()
            assertEquals(messageIdList.first(), Bookmarks.readCursor(adminId, CursorType.START))
            assertEquals(messageIdList.last(), Bookmarks.readCursor(adminId, CursorType.END))
        }
    }

    @Nested
    inner class DeleteBookmark {
        @Test
        fun `Deleting a message's bookmarks must only notify users who have bookmarked it`() {
            runBlocking {
                val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
                val chatId = GroupChats.create(setOf(adminId), listOf(user1Id, user2Id))
                val messageId = Messages.message(adminId, chatId)
                listOf(adminId, user1Id).forEach { Bookmarks.create(it, messageId) }
                awaitBrokering()
                val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                    .map { messagesNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
                Bookmarks.deleteBookmark(messageId)
                awaitBrokering()
                listOf(adminSubscriber, user1Subscriber).forEach { subscriber ->
                    val actual = subscriber.values().map { (it as UpdatedMessage).getMessageId() }
                    assertEquals(listOf(messageId), actual)
                }
                user2Subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class DeleteUserBookmark {
        @Test
        fun `Deleting a bookmark must only notify the deleter`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                Bookmarks.create(user1Id, messageId)
                awaitBrokering()
                val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                    .map { messagesNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
                Bookmarks.deleteUserBookmark(user1Id, messageId)
                awaitBrokering()
                val actual = user1Subscriber.values().map { (it as UpdatedMessage).getMessageId() }
                assertEquals(listOf(messageId), actual)
                user2Subscriber.assertNoValues()
            }
        }

        @Test
        fun `Deleting a non-existing bookmark mustn't cause anything to happen`() {
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatId = GroupChats.create(setOf(adminId))
                val messageId = Messages.message(adminId, chatId)
                awaitBrokering()
                val subscriber = messagesNotifier.subscribe(UserId(adminId)).flowable.subscribeWith(TestSubscriber())
                Bookmarks.deleteUserBookmark(adminId, messageId)
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class DeleteUserChat {
        @Test
        fun `Every message the user bookmarked in the chat must be unbookmarked`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            Bookmarks.create(userId, messageId)
            GroupChatUsers.removeUsers(chatId, userId)
            assertTrue(Bookmarks.readMessageIdList(userId).isEmpty())
        }

        @Test
        fun `Only the user must be notified of the unbookmarked messages`(): Unit = runBlocking {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(user1Id, user2Id))
            val messageId = Messages.message(adminId, chatId)
            Bookmarks.create(user1Id, messageId)
            awaitBrokering()
            val (user1Subscriber, user2Subscriber) =
                listOf(user1Id, user2Id).map {
                    messagesNotifier.subscribe(UserId(it)).flowable.subscribeWith(
                        TestSubscriber()
                    )
                }
            GroupChatUsers.removeUsers(chatId, user1Id)
            awaitBrokering()
            val actual = user1Subscriber.values().map { (it as UnbookmarkedChat).getChatId() }
            assertEquals(listOf(chatId), actual)
            user2Subscriber.assertNoValues()
        }
    }
}
