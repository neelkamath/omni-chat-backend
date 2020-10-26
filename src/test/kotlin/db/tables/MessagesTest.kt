package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import kotlin.test.*

@ExtendWith(DbExtension::class)
class MessagesTest {
    data class CreatedMessage(val creatorId: Int, val message: String)

    @Nested
    inner class IsInvalidBroadcast {
        @Test
        fun `Messaging in a private chat shouldn't count as an invalid broadcast`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertFalse(Messages.isInvalidBroadcast(user1Id, chatId))
        }

        @Test
        fun `Admins and users messaging in non-broadcast group chats shouldn't be invalid broadcasts`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertFalse(Messages.isInvalidBroadcast(userId, chatId))
            assertFalse(Messages.isInvalidBroadcast(adminId, chatId))
        }

        @Test
        fun `Only an admin should be able to message in a broadcast group chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId), isBroadcast = true)
            assertFalse(Messages.isInvalidBroadcast(adminId, chatId))
            assertTrue(Messages.isInvalidBroadcast(userId, chatId))
        }
    }

    @Nested
    inner class IsVisible {
        @Test
        fun `A nonexistent message shouldn't be said to be visible`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertFalse(Messages.isVisible(userId, messageId = 1))
        }

        @Test
        fun `The message shouldn't be visible if the user isn't in the chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertFalse(Messages.isVisible(userId, messageId))
        }

        @Test
        fun `The message should be visible if the user is in the group chat`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertTrue(Messages.isVisible(adminId, messageId))
        }

        private fun createChatWithMessage(shouldDelete: Boolean) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            if (shouldDelete) PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId)
            assertTrue(Messages.isVisible(user1Id, messageId))
        }

        @Test
        fun `The message should be visible if the user never deleted the private chat`() {
            createChatWithMessage(shouldDelete = false)
        }

        @Test
        fun `The message should be visible if it was sent after the user deleted the private chat`() {
            createChatWithMessage(shouldDelete = true)
        }

        @Test
        fun `The message shouldn't be visible if it was sent before the user deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFalse(Messages.isVisible(user1Id, messageId))
        }
    }

    @Nested
    inner class Search {
        @Test
        fun `Text messages should be searched case insensitively`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId, MessageText("Hi"))
            Messages.message(adminId, chatId, MessageText("Bye"))
            val messages = Messages.searchGroupChat(chatId, "hi", userId = adminId).map { it.node.messageId }
            assertEquals(listOf(messageId), messages)
        }

        @Test
        fun `Pic message captions should be searched case insensitively`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val message1 = CaptionedPic(pic, caption = MessageText("Hi"))
            val messageId = Messages.message(adminId, chatId, message1)
            val message2 = CaptionedPic(pic, caption = MessageText("Bye"))
            Messages.message(adminId, chatId, message2)
            val messages = Messages.searchGroupChat(chatId, "hi", userId = adminId).map { it.node.messageId }
            assertEquals(listOf(messageId), messages)
        }

        @Test
        fun `Poll message title and options should be searched case insensitively`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message1Options = listOf(MessageText("Burger King"), MessageText("Pizza Hut"))
            val message1 = PollInput(title = MessageText("Restaurant"), options = message1Options)
            val message1Id = Messages.message(adminId, chatId, message1)
            val message2Options = listOf(MessageText("Japanese Restaurant"), MessageText("Thai Restaurant"))
            val message2 = PollInput(MessageText("Title"), message2Options)
            val message2Id = Messages.message(adminId, chatId, message2)
            val message3Options = listOf(MessageText("option 1"), MessageText("option 2"))
            val message3 = PollInput(MessageText("Title"), message3Options)
            Messages.message(adminId, chatId, message3)
            val messages = Messages.searchGroupChat(chatId, "restaurant").map { it.node.messageId }
            assertEquals(listOf(message1Id, message2Id), messages)
        }

        @Test
        fun `Action messages should be searched by their text and actions case insensitively`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message1Id = Messages.message(
                    adminId,
                    chatId,
                    ActionMessageInput(MessageText("Order food."), listOf(MessageText("Pizza"), MessageText("Burger")))
            )
            val message2Id = Messages.message(
                    adminId,
                    chatId,
                    ActionMessageInput(MessageText("Pizza toppings?"), listOf(MessageText("Yes"), MessageText("No")))
            )
            Messages.message(
                    adminId,
                    chatId,
                    ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No")))
            )
            val messages = Messages.searchGroupChat(chatId, "pIzZa").map { it.node.messageId }
            assertEquals(listOf(message1Id, message2Id), messages)
        }

        @Test
        fun `Messages which don't contain text shouldn't be returned`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            Messages.message(adminId, chatId, Audio(ByteArray(1), Audio.Type.MP3))
            assertTrue(Messages.searchGroupChat(chatId, "query", userId = adminId).isEmpty())
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `Subscribers should receive notifications of created messages`(): Unit = runBlocking {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id))
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                    .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            repeat(3) { Messages.create(listOf(adminId, user1Id, user2Id).random(), chatId) }
            awaitBrokering()
            mapOf(adminId to adminSubscriber, user1Id to user1Subscriber, user2Id to user2Subscriber)
                    .forEach { (userId, subscriber) ->
                        val updates = Messages.readGroupChat(chatId, userId = userId).map { it.node.toNewTextMessage() }
                        subscriber.assertValueSequence(updates)
                    }
        }

        @Test
        fun `A subscriber should be notified of a new message in a private chat they just deleted`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                PrivateChatDeletions.create(chatId, user1Id)
                val subscriber =
                        messagesNotifier.safelySubscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
                val messageId = Messages.message(user2Id, chatId)
                awaitBrokering()
                Messages.readMessage(user1Id, messageId).toNewTextMessage().let(subscriber::assertValue)
            }
        }

        @Test
        fun `An exception should be thrown if the user isn't in the chat`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertFailsWith<IllegalArgumentException> { Messages.create(userId, chatId = 1) }
        }

        @Test
        fun `An exception should be thrown if a non-admin messages in a broadcast chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId), isBroadcast = true)
            assertFailsWith<IllegalArgumentException> { Messages.create(userId, chatId) }
        }

        @Test
        fun `An exception should be thrown if the context message isn't in the chat`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertFailsWith<IllegalArgumentException> { Messages.create(adminId, chatId, contextMessageId = 1) }
        }
    }

    @Nested
    inner class Read {
        @Test
        fun `Messages should be read in the order of their creation`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val createdMessages = listOf(
                    CreatedMessage(user1Id, "Hey"),
                    CreatedMessage(user2Id, "Hi!"),
                    CreatedMessage(user1Id, "I have a question"),
                    CreatedMessage(user1Id, "Is tomorrow a holiday?")
            )
            createdMessages.forEach { Messages.create(it.creatorId, chatId, MessageText(it.message)) }
            Messages.readPrivateChat(user1Id, chatId).forEachIndexed { index, message ->
                assertEquals(createdMessages[index].creatorId, message.node.sender.id)
                assertEquals(createdMessages[index].message, message.node.messageId.let(TextMessages::read).value)
            }
        }

        @Test
        fun `Private chats shouldn't show messages sent before it was deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId)
            Messages.create(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId)
            val messages = Messages.readPrivateChat(user1Id, chatId).map { it.node }
            assertEquals(listOf(Messages.readMessage(user1Id, messageId)), messages)
        }
    }

    @Nested
    inner class DeleteChatMessages {
        @Test
        fun `Deleting a chat containing message contexts should be deleted successfully`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextId = Messages.message(adminId, chatId)
            Messages.createTextMessage(adminId, chatId, MessageText("t"), contextId)
            Messages.deleteChat(chatId)
        }

        @Test
        fun `Deleting a message should set messages using it as a context to have a null context`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val contextId = Messages.message(userId, chatId)
            val messageId = Messages.message(adminId, chatId, contextMessageId = contextId)
            Messages.deleteUserChatMessages(chatId, userId)
            assertNull(Messages.readMessage(adminId, messageId).context.id)
        }
    }

    @Nested
    inner class DeleteChat {
        @Test
        fun `The subscriber should be notified of every message being deleted`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val subscriber =
                        messagesNotifier.safelySubscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
                Messages.deleteChat(chatId)
                awaitBrokering()
                subscriber.assertValue(DeletionOfEveryMessage(chatId))
            }
        }
    }

    @Nested
    inner class DeleteChatUntil {
        @Test
        fun `Every message should be deleted until the specified point`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            Messages.create(adminId, chatId)
            val now = LocalDateTime.now()
            val messageId = Messages.message(adminId, chatId)
            Messages.deleteChatUntil(chatId, now)
            assertEquals(listOf(messageId), Messages.readIdList(chatId))
        }

        @Test
        fun `Subscribers should be notified of the message deletion point`() {
            runBlocking {
                val adminId = createVerifiedUsers(1)[0].info.id
                val chatId = GroupChats.create(listOf(adminId))
                val subscriber =
                        messagesNotifier.safelySubscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
                val until = LocalDateTime.now()
                Messages.deleteChatUntil(chatId, until)
                awaitBrokering()
                subscriber.assertValue(MessageDeletionPoint(chatId, until))
            }
        }
    }

    @Nested
    inner class DeleteUserChatMessages {
        @Test
        fun `A subscriber should be notified when a user's messages have been deleted from the chat`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), listOf(userId))
                val subscriber =
                        messagesNotifier.safelySubscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
                Messages.deleteUserChatMessages(chatId, userId)
                awaitBrokering()
                subscriber.assertValue(UserChatMessagesRemoval(chatId, userId))
            }
        }
    }

    @Nested
    inner class DeleteUserMessages {
        @Test
        fun `Subscribers should be notified when every message the user sent is deleted`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = (1..2).map {
                val chatId = GroupChats.create(listOf(adminId), listOf(userId))
                Messages.create(userId, chatId)
                chatId
            }
            val (chat1Subscriber, chat2Subscriber) = (1..2)
                    .map { messagesNotifier.safelySubscribe(MessagesAsset(userId)).subscribeWith(TestSubscriber()) }
            Messages.deleteUserMessages(userId)
            awaitBrokering()
            listOf(chat1Subscriber, chat2Subscriber).forEach {
                it.assertValues(UserChatMessagesRemoval(chat1Id, userId), UserChatMessagesRemoval(chat2Id, userId))
            }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting a message should trigger a notification`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                val subscriber =
                        messagesNotifier.safelySubscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
                Messages.delete(messageId)
                awaitBrokering()
                subscriber.assertValue(DeletedMessage(chatId, messageId))
            }
        }
    }

    @Nested
    inner class ExistsFrom {
        @Test
        fun `Searching for messages sent after a particular time should be found`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertFalse(Messages.existsFrom(chatId, LocalDateTime.now()))
            val now = LocalDateTime.now()
            Messages.create(adminId, chatId)
            assertTrue(Messages.existsFrom(chatId, now))
        }
    }

    @Nested
    inner class ReadPrivateChat {
        @Test
        fun `Messages deleted by the user via a private chat deletion should only be visible to the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(user1Id, chatId)
            assertEquals(1, Messages.readPrivateChat(user1Id, chatId).size)
            assertEquals(2, Messages.readPrivateChat(user2Id, chatId).size)
        }
    }

    @Nested
    inner class ReadChat {
        @Test
        fun `Messages should only be retrieved from the specified chat`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val create = {
                GroupChats.create(listOf(adminId)).also { Messages.create(adminId, it) }
            }
            create()
            assertEquals(1, Messages.readGroupChat(create(), userId = adminId).size)
        }

        @Test
        fun `Messages should be retrieved in the order of their creation`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messagesIdList = (1..3).map { Messages.message(adminId, chatId) }
            assertEquals(messagesIdList, Messages.readGroupChat(chatId, userId = adminId).map { it.cursor })
        }

        @Test
        fun `Every message should be retrieved if neither the cursor nor the limit are supplied`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..3).map { Messages.message(adminId, chatId) }
            assertEquals(messageIdList, Messages.readGroupChat(chatId, userId = adminId).map { it.cursor })
        }

        @Test
        fun `The number of messages specified by the limit should be retrieved from before the cursor`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val last = 3
            val cursorIndex = 7
            val cursors = Messages
                    .readGroupChat(chatId, BackwardPagination(last, before = messageIdList[cursorIndex]), adminId)
                    .map { it.cursor }
            assertEquals(messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last), cursors)
        }

        @Test
        fun `"limit"ed messages from the last message should be retrieved when there's no cursor`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..5).map { Messages.message(adminId, chatId) }
            val last = 3
            val cursors = Messages.readGroupChat(chatId, BackwardPagination(last), adminId).map { it.cursor }
            assertEquals(messageIdList.takeLast(last), cursors)

        }

        @Test
        fun `Every message before the cursor should be retrieved when there's no limit`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messages = 5
            val messageIdList = (1..messages).map { Messages.message(adminId, chatId) }
            val index = 3
            val cursor = messageIdList[index]
            val cursors = Messages.readGroupChat(chatId, BackwardPagination(before = cursor), adminId).map { it.cursor }
            assertEquals(messageIdList.dropLast(messages - index), cursors)
        }

        @Test
        fun `Using a deleted message's cursor shouldn't cause pagination to behave differently`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val index = 5
            val deletedMessageId = messageIdList[index]
            Messages.delete(deletedMessageId)
            val last = 3
            val cursors = Messages
                    .readGroupChat(chatId, BackwardPagination(last, before = deletedMessageId), adminId)
                    .map { it.cursor }
            assertEquals(messageIdList.subList(index - last, index), cursors)
        }
    }

    @Nested
    inner class ReadPrivateChatConnection {
        @Test
        fun `Messages deleted via a private chat deletion shouldn't be retrieved only for the user who deleted it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message2Id = Messages.message(user1Id, chatId)
            val assert = { userId: Int, messageIdList: List<Int> ->
                assertEquals(messageIdList, Messages.readPrivateChatConnection(chatId, userId).edges.map { it.cursor })
            }
            assert(user1Id, listOf(message2Id))
            assert(user2Id, listOf(message1Id, message2Id))
        }
    }

    data class CreatedChat(val adminId: Int, val chatId: Int, val firstMessageId: Int, val secondMessageId: Int)

    @Nested
    inner class HasMessages {
        private fun createChat(): CreatedChat {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message = { Messages.message(adminId, chatId) }
            return CreatedChat(adminId, chatId, firstMessageId = message(), secondMessageId = message())
        }

        @Test
        fun `There shouldn't be messages before the first message`() {
            val (adminId, chatId, firstMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(before = firstMessageId), adminId)
                    .pageInfo
                    .hasPreviousPage
                    .let(::assertFalse)
        }

        @Test
        fun `There shouldn't be messages after the last message`() {
            val (adminId, chatId, _, lastMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(before = lastMessageId), adminId)
                    .pageInfo
                    .hasNextPage
                    .let(::assertFalse)
        }

        @Test
        fun `There should be messages before the last message`() {
            val (adminId, chatId, _, lastMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(last = 0, before = lastMessageId), adminId)
                    .pageInfo
                    .hasPreviousPage
                    .let(::assertTrue)
        }

        @Test
        fun `There should be messages after the first message`() {
            val (adminId, chatId, firstMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(before = firstMessageId), adminId)
                    .pageInfo
                    .hasNextPage
                    .let(::assertTrue)
        }
    }

    @Nested
    inner class ReadCursor {
        private fun assertCursor(hasMessage: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = if (hasMessage) Messages.message(adminId, chatId) else null
            with(Messages.readGroupChatConnection(chatId, userId = adminId).pageInfo) {
                assertEquals(messageId, startCursor)
                assertEquals(messageId, endCursor)
            }
        }

        @Test
        fun `Cursors should be null if there are no messages`() {
            assertCursor(hasMessage = false)
        }

        @Test
        fun `The cursor should be the same for both cursor types if there's only one message`() {
            assertCursor(hasMessage = true)
        }

        @Test
        fun `The first and last message IDs should be retrieved for the start and end cursors respectively`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..5).map { Messages.message(adminId, chatId) }
            with(Messages.readGroupChatConnection(chatId, userId = adminId).pageInfo) {
                assertEquals(messageIdList.first(), startCursor)
                assertEquals(messageIdList.last(), endCursor)
            }
        }
    }
}
