package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.time.LocalDateTime

class MessagesTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    data class CreatedMessage(val creator: String, val message: String)

    context("isVisible(Int, String)") {
        /**
         * Creates a private chat between two users, has the first user delete the chat if [shouldDelete], has the
         * second user send a message, and asserts that the first user can see the message.
         */
        fun createChatWithMessage(shouldDelete: Boolean) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            if (shouldDelete) PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(chatId, user2Id, "text")
            Messages.isVisible(messageId, user1Id).shouldBeTrue()
        }

        test("The message should be visible if the user never deleted the chat") {
            createChatWithMessage(shouldDelete = false)
        }

        test("The message should be visible if it was sent after the user deleted the chat") {
            createChatWithMessage(shouldDelete = true)
        }

        test("The message shouldn't be visible if it was sent before the user deleted the chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user2Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.isVisible(messageId, user1Id).shouldBeFalse()
        }
    }

    context("create(Int, String, String)") {
        test("Subscribers should receive notifications of created messages") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(user1Id, user2Id))
            val chatId = GroupChats.create(adminId, chat)
            val adminSubscriber = createMessageUpdatesSubscriber(adminId, chatId)
            val user1Subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            val user2Subscriber = createMessageUpdatesSubscriber(user2Id, chatId)
            repeat(3) { Messages.create(chatId, listOf(adminId, user1Id, user2Id).random(), "text") }
            val updates = Messages.readGroupChat(chatId).map { it.node }.toTypedArray()
            listOf(adminSubscriber, user1Subscriber, user2Subscriber).forEach { it.assertValues(*updates) }
        }

        test("An exception should be thrown if the user isn't in the chat") {
            val userId = createVerifiedUsers(1)[0].info.id
            shouldThrowExactly<IllegalArgumentException> { Messages.create(chatId = 1, userId = userId, text = "text") }
        }
    }

    context("read(Int)") {
        test("Messages should be read in the order of their creation") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val createdMessages = listOf(
                CreatedMessage(user1Id, "Hey"),
                CreatedMessage(user2Id, "Hi!"),
                CreatedMessage(user1Id, "I have a question"),
                CreatedMessage(user1Id, "Is tomorrow a holiday?")
            )
            createdMessages.forEach { Messages.create(chatId, it.creator, it.message) }
            Messages.readPrivateChat(chatId, user1Id).forEachIndexed { index, message ->
                message.node.sender.id shouldBe createdMessages[index].creator
                message.node.text shouldBe createdMessages[index].message
            }
        }

        test(
            """
            Given a chat deleted by the user which had activity after its deletion,
            when the chat is retrieved for the user,
            then it should exclude messages sent before its deletion
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.message(chatId, user1Id, "text")
            Messages.message(chatId, user2Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(chatId, user2Id, "text")
            Messages.readPrivateChat(chatId, user1Id).map { it.node } shouldBe listOf(Messages.read(messageId))
        }
    }

    context("deleteChat(Int)") {
        test("Deleting a chat should delete its messages and messages statuses") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            Messages.deleteChat(chatId)
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }

        test("The subscriber should be notified of every message being deleted, and then be unsubscribed") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            Messages.deleteChat(chatId)
            subscriber.assertValue(DeletionOfEveryMessage())
            subscriber.assertComplete()
        }
    }

    context("delete(Int, LocalDateTime)") {
        test("Every message and message status should be deleted until the specified point only") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(message1Id, user2Id, MessageStatus.READ)
            val now = LocalDateTime.now()
            val message2Id = Messages.message(chatId, user2Id, "text")
            MessageStatuses.create(message2Id, user1Id, MessageStatus.DELIVERED)
            Messages.deleteChatMessagesUntil(chatId, until = now)
            Messages.readIdList(chatId) shouldBe listOf(message2Id)
            MessageStatuses.count() shouldBe 1
        }
    }

    context("delete(Int, String)") {
        test("Every message and message status the user has in the chat should be deleted") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val message1Id = Messages.message(chatId, adminId, "text")
            MessageStatuses.create(message1Id, userId, MessageStatus.READ)
            val message2Id = Messages.message(chatId, userId, "text")
            MessageStatuses.create(message2Id, adminId, MessageStatus.READ)
            Messages.deleteUserChatMessages(chatId, userId)
            Messages.readIdList(chatId) shouldBe listOf(message1Id)
            MessageStatuses.count().shouldBeZero()
        }

        test("A subscriber should be notified when a user's messages have been deleted from the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val subscriber = createMessageUpdatesSubscriber(adminId, chatId)
            Messages.deleteUserChatMessages(chatId, userId)
            subscriber.assertValue(UserChatMessagesRemoval(userId))
        }
    }

    context("delete(String)") {
        fun createUtilizedPrivateChat(user1Id: String, user2Id: String): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(message1Id, user2Id, MessageStatus.READ)
            val message2Id = Messages.message(chatId, user2Id, "text")
            MessageStatuses.create(message2Id, user1Id, MessageStatus.READ)
            return chatId
        }

        fun createUtilizedGroupChat(user1Id: String, user2Id: String): Int {
            val chat = NewGroupChat("Title", userIdList = listOf(user2Id))
            val chatId = GroupChats.create(user1Id, chat)
            val message1Id = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(message1Id, user2Id, MessageStatus.READ)
            val message2Id = Messages.message(chatId, user2Id, "text")
            MessageStatuses.create(message2Id, user1Id, MessageStatus.READ)
            GroupChatUsers.removeUsers(chatId, listOf(user2Id))
            return chatId
        }

        test(
            """
            Given a user in a private chat, and a group chat they've left,
            when deleting the user's messages,
            then their messages, created message statues, and received message statuses should be deleted
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val privateChatId = createUtilizedPrivateChat(user1Id, user2Id)
            val groupChatId = createUtilizedGroupChat(user1Id, user2Id)
            Messages.deleteUserMessages(user2Id)
            val testMessages = { messages: List<MessageEdge> ->
                messages.map { it.node.sender.id } shouldBe listOf(user1Id)
                messages.flatMap { it.node.dateTimes.statuses }.map { it.user.id }.shouldBeEmpty()
            }
            testMessages(Messages.readPrivateChat(privateChatId, user2Id))
            testMessages(Messages.readGroupChat(groupChatId))
        }

        /**
         * Creates a group chat with the [adminId] and [userId], has the [userId] send a message in it, and returns the
         * [userId]'s [TestSubscriber] to the chat.
         */
        fun createChatMessageSubscriber(adminId: String, userId: String): TestSubscriber<MessageUpdates> {
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            Messages.create(chatId, userId, "text")
            return createMessageUpdatesSubscriber(userId, chatId)
        }

        test("Subscribers should be notified when every message the user sent is deleted") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat1Subscriber = createChatMessageSubscriber(adminId, userId)
            val chat2Subscriber = createChatMessageSubscriber(adminId, userId)
            Messages.deleteUserMessages(userId)
            val removal = UserChatMessagesRemoval(userId)
            chat1Subscriber.assertValue(removal)
            chat2Subscriber.assertValue(removal)
        }
    }

    context("delete(Int)") {
        test("Deleting a message should delete it and its statuses") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val messageId = Messages.message(chatId, adminId, "text")
            MessageStatuses.create(messageId, userId, MessageStatus.DELIVERED)
            Messages.delete(messageId)
            Messages.readIdList(chatId).shouldBeEmpty()
            MessageStatuses.count().shouldBeZero()
        }

        test("Deleting a message should trigger a notification") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            Messages.delete(messageId)
            subscriber.assertValue(DeletedMessage(messageId))
        }
    }

    context("existsFrom(Int, LocalDateTime)") {
        test(
            """
            Given messages sent after a particular time,
            when searching for messages from that time,
            then they should be found
            """
        ) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val now = LocalDateTime.now()
            Messages.create(chatId, adminId, "text")
            Messages.existsFrom(chatId, now).shouldBeTrue()
        }

        test(
            """
            Given messages were only sent before a particular time, 
            when searching for messages from that time, 
            then none should be found
            """
        ) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            Messages.create(chatId, adminId, "text")
            Messages.existsFrom(chatId, LocalDateTime.now()).shouldBeFalse()
        }
    }

    context("readPrivateChat(Int, String, BackwardPagination?)") {
        test("Messages deleted by the user via a private chat deletion should only be visible to the other user") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(chatId, user1Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(chatId, user1Id, "text")
            Messages.readPrivateChat(chatId, user1Id) shouldHaveSize 1
            Messages.readPrivateChat(chatId, user2Id) shouldHaveSize 2
        }
    }

    context("readChat(Int, BackwardPagination?, Op<Boolean>?)") {
        test("Messages should only be retrieved from the specified chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val create = {
                GroupChats.create(adminId, NewGroupChat("Title")).also { Messages.create(it, adminId, "text") }
            }
            create()
            Messages.readGroupChat(create()) shouldHaveSize 1
        }

        test("Messages should be retrieved in the order of their creation.") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messagesIdList = (1..3).map { Messages.message(chatId, adminId, "text") }
            Messages.readGroupChat(chatId).map { it.cursor } shouldBe messagesIdList
        }

        test("Every message should be retrieved if neither the cursor nor the limit are supplied") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messageIdList = (1..3).map { Messages.message(chatId, adminId, "text") }
            Messages.readGroupChat(chatId).map { it.cursor } shouldBe messageIdList
        }

        test(
            """
            Given both a limit and cursor,
            when retrieving messages,
            then the number of messages specified by the limit should be retrieved from before the cursor
            """
        ) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messageIdList = (1..10).map { Messages.message(chatId, adminId, "text") }
            val last = 3
            val cursorIndex = 7
            Messages
                .readGroupChat(chatId, BackwardPagination(last, before = messageIdList[cursorIndex]))
                .map { it.cursor }
                .shouldBe(messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last))
        }

        test(
            """
            Given a limit without a cursor,
            when retrieving messages,
            then the number of messages specified by the limit from the last message should be retrieved 
            """
        ) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messageIdList = (1..5).map { Messages.message(chatId, adminId, "text") }
            val last = 3
            Messages.readGroupChat(chatId, BackwardPagination(last)).map { it.cursor } shouldBe
                    messageIdList.takeLast(last)
        }

        test(
            """
            Given a cursor without a limit, 
            when retrieving messages, 
            then every message before the cursor should be retrieved
            """
        ) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messages = 5
            val messageIdList = (1..messages).map { Messages.message(chatId, adminId, "text") }
            val index = 3
            val cursor = messageIdList[index]
            Messages.readGroupChat(chatId, BackwardPagination(before = cursor)).map { it.cursor } shouldBe
                    messageIdList.dropLast(messages - index)
        }

        test("Using a deleted message's cursor shouldn't cause pagination to behave differently") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messageIdList = (1..10).map { Messages.message(chatId, adminId, "text") }
            val index = 5
            val deletedMessageId = messageIdList[index]
            Messages.delete(deletedMessageId)
            val last = 3
            Messages
                .readGroupChat(chatId, BackwardPagination(last, before = deletedMessageId))
                .map { it.cursor } shouldBe messageIdList.subList(index - last, index)
        }
    }

    context("readPrivateChatConnection(Int, String, BackwardPagination?)") {
        test("Messages deleted via a private chat deletion shouldn't be retrieved only for the user who deleted it") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(chatId, user1Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            val message2Id = Messages.message(chatId, user1Id, "text")
            val assert = { userId: String, messageIdList: List<Int> ->
                Messages.readPrivateChatConnection(chatId, userId).edges.map { it.cursor } shouldBe messageIdList
            }
            assert(user1Id, listOf(message2Id))
            assert(user2Id, listOf(message1Id, message2Id))
        }
    }

    context("hasMessages(Int, Int, Chronology, Op<Boolean>?)") {
        data class CreatedChat(val chatId: Int, val firstMessageId: Int, val secondMessageId: Int)

        fun createChat(): CreatedChat {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val message = { Messages.message(chatId, adminId, "text") }
            return CreatedChat(chatId, firstMessageId = message(), secondMessageId = message())
        }

        test("There shouldn't be messages before the first message") {
            val (chatId, firstMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(before = firstMessageId))
                .pageInfo
                .hasPreviousPage
                .shouldBeFalse()
        }

        test("There shouldn't be messages after the last message") {
            val (chatId, _, lastMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(before = lastMessageId))
                .pageInfo
                .hasNextPage
                .shouldBeFalse()
        }

        test("There should be messages before the last message") {
            val (chatId, _, lastMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(last = 0, before = lastMessageId))
                .pageInfo
                .hasPreviousPage
                .shouldBeTrue()
        }

        test("There should be messages after the first message") {
            val (chatId, firstMessageId) = createChat()
            Messages.readGroupChatConnection(chatId, BackwardPagination(before = firstMessageId))
                .pageInfo
                .hasNextPage
                .shouldBeTrue()
        }
    }

    context("readCursor(Int, CursorType, Filter)") {
        fun assertCursor(hasMessage: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messageId = if (hasMessage) Messages.message(chatId, adminId, "text") else null
            with(Messages.readGroupChatConnection(chatId).pageInfo) {
                startCursor shouldBe messageId
                endCursor shouldBe messageId
            }
        }

        test("Cursors should be null if there are no messages") { assertCursor(hasMessage = false) }

        test("The cursor should be the same for both cursor types if there's only one message") {
            assertCursor(hasMessage = true)
        }

        test("The first and last message IDs should be retrieved for the start and end cursors respectively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val messageIdList = (1..5).map { Messages.message(chatId, adminId, "text") }
            with(Messages.readGroupChatConnection(chatId).pageInfo) {
                startCursor shouldBe messageIdList.first()
                endCursor shouldBe messageIdList.last()
            }
        }
    }
}