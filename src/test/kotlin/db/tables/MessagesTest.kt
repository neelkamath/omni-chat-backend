package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedMessage
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.NewTextMessage
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UserChatMessagesRemoval
import com.neelkamath.omniChatBackend.graphql.routing.ActionMessageInput
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.graphql.routing.PollInput
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import kotlin.test.*

/** Sends the [message] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(
    userId: Int,
    chatId: Int,
    message: MessageText = MessageText("t"),
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createTextMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: Audio,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createAudioMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: CaptionedPic,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createPicMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: PollInput,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createPollMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: ActionMessageInput,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createActionMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createGroupChatInviteMessage(userId, chatId, invitedChatId, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

@ExtendWith(DbExtension::class)
class MessagesTest {
    @Nested
    inner class IsInvalidBroadcast {
        @Test
        fun `Messaging in a private chat mustn't count as an invalid broadcast`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertFalse(Messages.isInvalidBroadcast(user1Id, chatId))
        }

        @Test
        fun `Admins and users messaging in non-broadcast group chats mustn't be invalid broadcasts`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            assertFalse(Messages.isInvalidBroadcast(userId, chatId))
            assertFalse(Messages.isInvalidBroadcast(adminId, chatId))
        }

        @Test
        fun `Only an admin must be able to message in a broadcast group chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId), isBroadcast = true)
            assertFalse(Messages.isInvalidBroadcast(adminId, chatId))
            assertTrue(Messages.isInvalidBroadcast(userId, chatId))
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `Subscribers must receive notifications of created messages`(): Unit = runBlocking {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(user1Id, user2Id))
            val (adminSubscriber, user1Subscriber, user2Subscriber) = setOf(adminId, user1Id, user2Id)
                .map { messagesNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
            val messageIdList = (1..3).map { Messages.message(setOf(adminId, user1Id, user2Id).random(), chatId) }
            awaitBrokering()
            setOf(adminSubscriber, user1Subscriber, user2Subscriber).forEach { subscriber ->
                val actual = subscriber.values().map { (it as NewTextMessage).getMessageId() }
                assertEquals(messageIdList, actual)
            }
        }

        @Test
        fun `Authenticated subscribers must be notified of a new message in a private chat they just deleted`(): Unit =
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
                val chatId = PrivateChats.create(user1Id, user2Id)
                PrivateChatDeletions.create(chatId, user1Id)
                awaitBrokering()
                val subscriber = messagesNotifier.subscribe(UserId(user1Id)).flowable.subscribeWith(TestSubscriber())
                val messageId = Messages.message(user2Id, chatId)
                awaitBrokering()
                val actual = subscriber.values().map { (it as NewTextMessage).getMessageId() }
                assertEquals(listOf(messageId), actual)
            }

        @Test
        fun `Unauthenticated subscribers must be notified of new messages in public chats`(): Unit = runBlocking {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            awaitBrokering()
            val subscriber = chatMessagesNotifier.subscribe(ChatId(chatId)).flowable.subscribeWith(TestSubscriber())
            val messageId = Messages.message(adminId, chatId)
            awaitBrokering()
            val actual = subscriber.values().map { (it as NewTextMessage).getMessageId() }
            assertEquals(listOf(messageId), actual)
        }

        @Test
        fun `An exception must be thrown if the user isn't in the chat`() {
            val userId = createVerifiedUsers(1).first().userId
            assertFailsWith<IllegalArgumentException> { Messages.message(userId, chatId = 1) }
        }

        @Test
        fun `An exception must be thrown if a non-admin messages in a broadcast chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId), isBroadcast = true)
            assertFailsWith<IllegalArgumentException> { Messages.message(userId, chatId) }
        }

        @Test
        fun `An exception must be thrown if the context message isn't in the chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertFailsWith<IllegalArgumentException> { Messages.message(adminId, chatId, contextMessageId = 1) }
        }
    }

    @Nested
    inner class Search {
        @Test
        fun `Given both searchable and non-searchable messages in a chat appearing one after the other, when searching for searchable messages, then they must get returned`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val query = "matched"
            val messageIdList = (1..10).map {
                val message = Audio(ByteArray(1))
                Messages.message(adminId, chatId, message)
                Messages.message(adminId, chatId, MessageText(query))
            }
            val last = 3
            val index = 7
            val expected = messageIdList.subList(index - last, index).toLinkedHashSet()
            val actual =
                Messages.searchGroupChat(chatId, query, BackwardPagination(last, before = messageIdList[index]))
            assertEquals(expected, actual)
        }

        @Test
        fun `Text messages must be searched case insensitively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId, MessageText("Hi"))
            Messages.message(adminId, chatId, MessageText("Bye"))
            assertEquals(linkedHashSetOf(messageId), Messages.searchGroupChat(chatId, "hi"))
        }

        @Test
        fun `Pic message captions must be searched case insensitively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val pic = readPic("76px×57px.jpg")
            val message1 = CaptionedPic(pic, caption = MessageText("Hi"))
            val messageId = Messages.message(adminId, chatId, message1)
            val message2 = CaptionedPic(pic, caption = MessageText("Bye"))
            Messages.message(adminId, chatId, message2)
            assertEquals(linkedHashSetOf(messageId), Messages.searchGroupChat(chatId, "hi"))
        }

        @Test
        fun `Poll message question and options must be searched case insensitively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val message1Options = listOf(MessageText("Burger King"), MessageText("Pizza Hut"))
            val message1 = PollInput(question = MessageText("Restaurant"), options = message1Options)
            val message1Id = Messages.message(adminId, chatId, message1)
            val message2Options = listOf(MessageText("Japanese Restaurant"), MessageText("Thai Restaurant"))
            val message2 = PollInput(MessageText("Question"), message2Options)
            val message2Id = Messages.message(adminId, chatId, message2)
            val message3Options = listOf(MessageText("option 1"), MessageText("option 2"))
            val message3 = PollInput(MessageText("Question"), message3Options)
            Messages.message(adminId, chatId, message3)
            assertEquals(linkedHashSetOf(message1Id, message2Id), Messages.searchGroupChat(chatId, "restaurant"))
        }

        @Test
        fun `Action messages must be searched by their text and actions case insensitively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val message1 =
                ActionMessageInput(MessageText("Order food."), listOf(MessageText("Pizza"), MessageText("Burger")))
            val message1Id = Messages.message(adminId, chatId, message1)
            val message2 =
                ActionMessageInput(MessageText("Pizza toppings?"), listOf(MessageText("Yes"), MessageText("No")))
            val message2Id = Messages.message(adminId, chatId, message2)
            val message3 =
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No")))
            Messages.message(adminId, chatId, message3)
            assertEquals(linkedHashSetOf(message1Id, message2Id), Messages.searchGroupChat(chatId, "pIzZa"))
        }

        @Test
        fun `Messages which don't contain text mustn't be returned`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            Messages.message(adminId, chatId, Audio(ByteArray(1)))
            assertTrue(Messages.searchGroupChat(chatId, query = "").isEmpty())
        }

        @Test
        fun `Given items, when requesting items with neither a limit nor a cursor, then every item must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }.toLinkedHashSet()
            assertEquals(messageIdList, Messages.searchGroupChat(chatId, query = ""))
        }

        @Test
        fun `Given items, when requesting items with a limit and cursor, then the number of items specified by the limit must be returned from before the cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }.toLinkedHashSet()
            val last = 3
            val index = 7
            val pagination = BackwardPagination(last, before = messageIdList.elementAt(index))
            val actual = Messages.searchGroupChat(chatId, query = "", pagination)
            assertEquals(messageIdList.subList(index - last, index), actual)
        }

        @Test
        fun `Given items, when requesting items with a limit but no cursor, then the number of items specified by the limit from the last item must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }.toLinkedHashSet()
            val last = 3
            val actual = Messages.searchGroupChat(chatId, query = "", BackwardPagination(last))
            assertEquals(messageIdList.takeLast(last), actual)
        }

        @Test
        fun `Given items, when requesting items with a cursor but no limit, then every item before the cursor must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val index = 7
            val pagination = BackwardPagination(before = messageIdList[index])
            val actual = Messages.searchGroupChat(chatId, query = "", pagination)
            assertEquals(messageIdList.take(index).toLinkedHashSet(), actual)
        }

        @Test
        fun `Given items, when requesting items with the first item's cursor but no limit, then zero items must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[0]
            val actual = Messages.searchGroupChat(chatId, query = "", BackwardPagination(before = cursor)).size
            assertEquals(0, actual)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the last three items before item 6, then items 2, 3, and 5 must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            Messages.delete(messageIdList[3])
            val pagination = BackwardPagination(last = 3, before = messageIdList[5])
            val actual = Messages.searchGroupChat(chatId, query = "", pagination)
            assertEquals(linkedHashSetOf(messageIdList[1], messageIdList[2], messageIdList[4]), actual)
        }

        @Test
        fun `Given items 1-10 where item 6 has been deleted, when requesting items using the deleted item's cursor, then items 1-5 must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val messageId = messageIdList[5]
            Messages.delete(messageId)
            val actual = Messages.searchGroupChat(chatId, query = "", BackwardPagination(before = messageId))
            assertEquals(messageIdList.take(5).toLinkedHashSet(), actual)
        }
    }

    @Nested
    inner class ReadPrivateChat {
        @Test
        fun `Messages deleted by the user via a private chat deletion must only be visible to the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.message(user1Id, chatId)
            assertEquals(1, Messages.readPrivateChat(user1Id, chatId).size)
            assertEquals(2, Messages.readPrivateChat(user2Id, chatId).size)
        }
    }

    /** A group [chatId] and its [messageIdList] which sorted in ascending order. */
    private data class PaginatedChat(val chatId: Int, val messageIdList: LinkedHashSet<Int>)

    @Nested
    inner class ReadChat {
        @Test
        fun `Messages must only be retrieved from the specified chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val create = {
                GroupChats.create(setOf(adminId)).also { Messages.message(adminId, it) }
            }
            create()
            assertEquals(1, Messages.readGroupChat(create()).size)
        }

        private fun createPaginatedChat(messages: Int = 10): PaginatedChat {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..messages).map { Messages.message(adminId, chatId) }.toLinkedHashSet()
            return PaginatedChat(chatId, messageIdList)
        }

        @Test
        fun `Every message must be retrieved if neither the cursor nor the limit get supplied`() {
            val (chatId, messageIdList) = createPaginatedChat()
            assertEquals(messageIdList, Messages.readGroupChat(chatId))
        }

        @Test
        fun `The number of messages specified by the limit must be retrieved from before the cursor`() {
            val (chatId, messageIdList) = createPaginatedChat()
            val last = 3
            val cursorIndex = 7
            val pagination = BackwardPagination(last, before = messageIdList.elementAt(cursorIndex))
            val actual = Messages.readGroupChat(chatId, pagination)
            assertEquals(messageIdList.subList(cursorIndex - last, cursorIndex), actual)
        }

        @Test
        fun `A limited number of messages from the last message must be retrieved when there's no cursor`() {
            val (chatId, messageIdList) = createPaginatedChat()
            val last = 3
            val actual = Messages.readGroupChat(chatId, BackwardPagination(last))
            assertEquals(messageIdList.takeLast(last), actual)
        }

        @Test
        fun `Every message before the cursor must be retrieved when there's no limit`() {
            val (chatId, messageIdList) = createPaginatedChat()
            val index = 3
            val cursor = messageIdList.elementAt(index)
            val actual = Messages.readGroupChat(chatId, BackwardPagination(before = cursor))
            assertEquals(messageIdList.dropLast(messageIdList.size - index), actual)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the last three items before item 6, then items 2, 3, and 5 must be retrieved`() {
            val (chatId, messageIdList) = createPaginatedChat()
            Messages.delete(messageIdList.elementAt(3))
            val expected = setOf(1, 2, 4).map(messageIdList::elementAt).toLinkedHashSet()
            val pagination = BackwardPagination(last = 3, before = messageIdList.elementAt(5))
            assertEquals(expected, Messages.readGroupChat(chatId, pagination))
        }

        @Test
        fun `Zero items must be retrieved when using the first item's cursor`() {
            val (chatId, messageIdList) = createPaginatedChat()
            val pagination = BackwardPagination(before = messageIdList.first())
            assertEquals(0, Messages.readGroupChat(chatId, pagination).size)
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (chatId, messageIdList) = createPaginatedChat()
            val index = 4
            val messageId = messageIdList.elementAt(index)
            Messages.delete(messageId)
            val messages = Messages.readGroupChat(chatId, BackwardPagination(before = messageId))
            assertEquals(messageIdList.take(index).toLinkedHashSet(), messages)
        }
    }

    @Nested
    inner class DeleteChatMessages {
        @Test
        fun `Deleting a chat containing message contexts must be deleted successfully`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            Messages.message(adminId, chatId, contextMessageId = contextMessageId)
            Messages.deleteChat(chatId)
        }

        @Test
        fun `Deleting a message must set messages using it as a context to have a 'null' context`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            val contextId = Messages.message(userId, chatId)
            val messageId = Messages.message(adminId, chatId, contextMessageId = contextId)
            Messages.deleteUserChatMessages(chatId, userId)
            assertNull(Messages.readContextMessageId(messageId))
        }
    }

    @Nested
    inner class DeleteChatUntil {
        @Test
        fun `Every message must be deleted until the specified point`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            Messages.message(adminId, chatId)
            val now = LocalDateTime.now()
            val messageId = Messages.message(adminId, chatId)
            Messages.deleteChatUntil(chatId, now)
            assertEquals(setOf(messageId), Messages.readIdList(chatId))
        }
    }

    @Nested
    inner class DeleteUserChatMessages {
        @Test
        fun `Authenticated subscribers must be notified when a user's messages have been deleted from a non-public chat`(): Unit =
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
                val chatId = GroupChats.create(setOf(adminId), setOf(userId))
                val subscriber = messagesNotifier.subscribe(UserId(adminId)).flowable.subscribeWith(TestSubscriber())
                Messages.deleteUserChatMessages(chatId, userId)
                awaitBrokering()
                val values = subscriber.values().map { it as UserChatMessagesRemoval }
                assertEquals(listOf(chatId), values.map { it.getChatId() })
                assertEquals(listOf(userId), values.map { it.getUserId() })
            }

        @Test
        fun `Unauthenticated subscribers must receive notifications regarding deleted public chat messages`(): Unit =
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
                val chatId = GroupChats.create(setOf(adminId), setOf(userId), publicity = GroupChatPublicity.PUBLIC)
                Messages.message(userId, chatId)
                awaitBrokering()
                val subscriber = chatMessagesNotifier.subscribe(ChatId(chatId)).flowable.subscribeWith(TestSubscriber())
                Messages.deleteUserChatMessages(chatId, userId)
                awaitBrokering()
                val actual = subscriber.values().map { (it as UserChatMessagesRemoval).getChatId() }
                assertEquals(listOf(chatId), actual)
            }
    }

    @Nested
    inner class DeleteUserMessages {
        @Test
        fun `Subscribers must be notified when every message the user sent is deleted`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val (chat1Id, chat2Id) = (1..2).map {
                val chatId = GroupChats.create(setOf(adminId), setOf(userId))
                Messages.message(userId, chatId)
                chatId
            }
            awaitBrokering()
            val (chat1Subscriber, chat2Subscriber) =
                (1..2).map { messagesNotifier.subscribe(UserId(userId)).flowable.subscribeWith(TestSubscriber()) }
            Messages.deleteUserMessages(userId)
            awaitBrokering()
            setOf(chat1Subscriber, chat2Subscriber).forEach { subscriber ->
                val values = subscriber.values().map { it as UserChatMessagesRemoval }
                assertEquals(listOf(chat1Id, chat2Id), values.map { it.getChatId() })
                assertEquals(listOf(userId, userId), values.map { it.getUserId() })
            }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting a message must trigger a notification for authenticated subscribers`(): Unit = runBlocking {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            awaitBrokering()
            val subscriber = messagesNotifier.subscribe(UserId(user1Id)).flowable.subscribeWith(TestSubscriber())
            Messages.delete(messageId)
            awaitBrokering()
            val values = subscriber.values().map { it as DeletedMessage }
            assertEquals(listOf(chatId), values.map { it.getChatId() })
            assertEquals(listOf(messageId), values.map { it.getMessageId() })
        }

        @Test
        fun `Deleting a message must trigger a notification for unauthenticated users subscribed to a public chat`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
                val messageId = Messages.message(adminId, chatId)
                awaitBrokering()
                val subscriber = chatMessagesNotifier.subscribe(ChatId(chatId)).flowable.subscribeWith(TestSubscriber())
                Messages.delete(messageId)
                awaitBrokering()
                val actual = subscriber.values().map { (it as DeletedMessage).getMessageId() }
                assertEquals(listOf(messageId), actual)
            }
    }

    @Nested
    inner class IsExistingFrom {
        @Test
        fun `Searching for messages sent after a particular time must be found`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertFalse(Messages.isExistingFrom(chatId, LocalDateTime.now()))
            val now = LocalDateTime.now()
            Messages.message(adminId, chatId)
            assertTrue(Messages.isExistingFrom(chatId, now))
        }
    }

    @Nested
    inner class ReadPrivateChatCursor {
        @Test
        fun `The start cursor must be the first message if the private chat hasn't been deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            Messages.message(user1Id, chatId)
            assertEquals(messageId, Messages.readPrivateChatCursor(user1Id, chatId, CursorType.START))
            assertEquals(messageId, Messages.readPrivateChatCursor(user2Id, chatId, CursorType.START))
        }

        @Test
        fun `The start cursor must be different for each user if one of them deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message2Id = Messages.message(user1Id, chatId)
            assertEquals(message1Id, Messages.readPrivateChatCursor(user2Id, chatId, CursorType.START))
            assertEquals(message2Id, Messages.readPrivateChatCursor(user1Id, chatId, CursorType.START))
        }
    }

    @Nested
    inner class ReadCursor {
        @Test
        fun `The start and end cursors must point to the first and last items respectively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            assertEquals(messageIdList.first(), Messages.readGroupChatCursor(chatId, CursorType.START))
            assertEquals(messageIdList.last(), Messages.readGroupChatCursor(chatId, CursorType.END))
        }

        @Test
        fun `The start and end cursors must be 'null' if there are no messages`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertNull(Messages.readGroupChatCursor(chatId, CursorType.START))
            assertNull(Messages.readGroupChatCursor(chatId, CursorType.END))
        }
    }

    @Nested
    inner class IsVisible {
        @Test
        fun `The message must be visible if the chat is public even if the user ID is 'null'`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            assertTrue(Messages.isVisible(userId = null, messageId))
        }

        @Test
        fun `The message must not be visible if the user is 'null', and the chat isn't public`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertFalse(Messages.isVisible(userId = null, messageId))
        }

        @Test
        fun `A non-existing message mustn't be said to be visible`() {
            val userId = createVerifiedUsers(1).first().userId
            assertFalse(Messages.isVisible(userId, messageId = 1))
        }

        @Test
        fun `The message mustn't be visible if the user isn't in the chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertFalse(Messages.isVisible(userId, messageId))
        }

        @Test
        fun `The message must be visible if the user is in the group chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertTrue(Messages.isVisible(adminId, messageId))
        }

        private fun createChatWithMessage(mustDelete: Boolean) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            if (mustDelete) PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId)
            assertTrue(Messages.isVisible(user1Id, messageId))
        }

        @Test
        fun `The message must be visible if the user never deleted the private chat`(): Unit =
            createChatWithMessage(mustDelete = false)

        @Test
        fun `The message must be visible if it was sent after the user deleted the private chat`(): Unit =
            createChatWithMessage(mustDelete = true)

        @Test
        fun `The message mustn't be visible if it was sent before the user deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFalse(Messages.isVisible(user1Id, messageId))
        }
    }
}
