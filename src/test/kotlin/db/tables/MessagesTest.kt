package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.messagesBroker
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

class MessagesTest : FunSpec({
    data class CreatedMessage(val creatorId: Int, val message: String)

    context("isVisible(Int, String)") {
        test("A nonexistent message shouldn't be said to be visible") {
            val userId = createVerifiedUsers(1)[0].info.id
            Messages.isVisible(userId, messageId = 1).shouldBeFalse()
        }

        test("The message shouldn't be visible if the user isn't in the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId)
            val messageId = Messages.message(adminId, chatId, TextMessage("t"))
            Messages.isVisible(userId, messageId).shouldBeFalse()
        }

        test("The message should be visible if the user is in the group chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val messageId = Messages.message(adminId, chatId, TextMessage("t"))
            Messages.isVisible(adminId, messageId).shouldBeTrue()
        }

        fun createChatWithMessage(shouldDelete: Boolean) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            if (shouldDelete) PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId, TextMessage("t"))
            Messages.isVisible(user1Id, messageId).shouldBeTrue()
        }

        test("The message should be visible if the user never deleted the private chat") {
            createChatWithMessage(shouldDelete = false)
        }

        test("The message should be visible if it was sent after the user deleted the private chat") {
            createChatWithMessage(shouldDelete = true)
        }

        test("The message shouldn't be visible if it was sent before the user deleted the chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId, TextMessage("t"))
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.isVisible(user1Id, messageId).shouldBeFalse()
        }
    }

    context("create(Int, String, TextMessage)") {
        test("Subscribers should receive notifications of created messages") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(user1Id, user2Id))
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { messagesBroker.subscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            repeat(3) { Messages.create(listOf(adminId, user1Id, user2Id).random(), chatId, TextMessage("t")) }
            mapOf(adminId to adminSubscriber, user1Id to user1Subscriber, user2Id to user2Subscriber)
                .forEach { (userId, subscriber) ->
                    val updates = Messages.readGroupChat(userId, chatId).map { it.node.toNewMessage() }
                    subscriber.assertValueSequence(updates)
                }
        }

        test("A subscriber should be notified of a new message in a private chat they just deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            val subscriber = messagesBroker.subscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            val messageId = Messages.message(user2Id, chatId, TextMessage("t"))
            subscriber.assertValue(Messages.readMessage(user1Id, messageId).toNewMessage())
        }

        test("An exception should be thrown if the user isn't in the chat") {
            val userId = createVerifiedUsers(1)[0].info.id
            shouldThrowExactly<IllegalArgumentException> {
                Messages.create(userId, chatId = 1, text = TextMessage("t"))
            }
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
            createdMessages.forEach { Messages.create(it.creatorId, chatId, TextMessage(it.message)) }
            Messages.readPrivateChat(user1Id, chatId).forEachIndexed { index, message ->
                message.node.sender.id shouldBe createdMessages[index].creatorId
                message.node.text.value shouldBe createdMessages[index].message
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
            Messages.create(user1Id, chatId, TextMessage("t"))
            Messages.create(user2Id, chatId, TextMessage("t"))
            PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId, TextMessage("t"))
            Messages.readPrivateChat(user1Id, chatId).map { it.node } shouldBe listOf(
                Messages.readMessage(
                    user1Id,
                    messageId
                )
            )
        }
    }

    context("deleteChat(Int)") {
        test("Deleting a chat should delete its messages and messages statuses") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId, TextMessage("t"))
            MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
            Messages.deleteChat(chatId)
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }

        test("The subscriber should be notified of every message being deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val subscriber = messagesBroker.subscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            Messages.deleteChat(chatId)
            subscriber.assertValue(DeletionOfEveryMessage(chatId))
        }
    }

    context("deleteMessagesUntil(Int, LocalDateTime)") {
        test("Every message and message status should be deleted until the specified point only") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(user1Id, chatId, TextMessage("t"))
            MessageStatuses.create(user2Id, message1Id, MessageStatus.READ)
            val now = LocalDateTime.now()
            val message2Id = Messages.message(user2Id, chatId, TextMessage("t"))
            MessageStatuses.create(user1Id, message2Id, MessageStatus.DELIVERED)
            Messages.deleteMessagesUntil(chatId, until = now)
            Messages.readIdList(chatId) shouldBe listOf(message2Id)
            MessageStatuses.count() shouldBe 1
        }

        test("Subscribers should be notified of the message deletion point") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val subscriber = messagesBroker.subscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
            val until = LocalDateTime.now()
            Messages.deleteMessagesUntil(chatId, until)
            subscriber.assertValue(MessageDeletionPoint(chatId, until))
        }
    }

    context("deleteUserChatMessages(Int, String)") {
        test("Every message and message status the user has in the chat should be deleted") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val message1Id = Messages.message(adminId, chatId, TextMessage("t"))
            MessageStatuses.create(userId, message1Id, MessageStatus.READ)
            val message2Id = Messages.message(userId, chatId, TextMessage("t"))
            MessageStatuses.create(adminId, message2Id, MessageStatus.READ)
            Messages.deleteUserChatMessages(chatId, userId)
            Messages.readIdList(chatId) shouldBe listOf(message1Id)
            MessageStatuses.count().shouldBeZero()
        }

        test("A subscriber should be notified when a user's messages have been deleted from the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val subscriber = messagesBroker.subscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
            Messages.deleteUserChatMessages(chatId, userId)
            subscriber.assertValue(UserChatMessagesRemoval(chatId, userId))
        }
    }

    context("deleteUserMessages(String)") {
        fun createUtilizedPrivateChat(user1Id: Int, user2Id: Int): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(user1Id, chatId, TextMessage("t"))
            MessageStatuses.create(user2Id, message1Id, MessageStatus.READ)
            val message2Id = Messages.message(user2Id, chatId, TextMessage("t"))
            MessageStatuses.create(user1Id, message2Id, MessageStatus.READ)
            return chatId
        }

        fun createUtilizedGroupChat(user1Id: Int, user2Id: Int): Int {
            val chatId = GroupChats.create(user1Id, buildNewGroupChat(user2Id))
            val message1Id = Messages.message(user1Id, chatId, TextMessage("t"))
            MessageStatuses.create(user2Id, message1Id, MessageStatus.READ)
            val message2Id = Messages.message(user2Id, chatId, TextMessage("t"))
            MessageStatuses.create(user1Id, message2Id, MessageStatus.READ)
            GroupChatUsers.removeUsers(chatId, user2Id)
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
            listOf(Messages.readPrivateChat(user2Id, privateChatId), Messages.readGroupChat(user2Id, groupChatId))
                .forEach { messages ->
                    messages.map { it.node.sender.id } shouldBe listOf(user1Id)
                    messages.flatMap { it.node.dateTimes.statuses }.map { it.user.id }.shouldBeEmpty()
                }
        }

        test("Subscribers should be notified when every message the user sent is deleted") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = (1..2).map {
                val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
                Messages.create(userId, chatId, TextMessage("t"))
                chatId
            }
            val (chat1Subscriber, chat2Subscriber) = (1..2)
                .map { messagesBroker.subscribe(MessagesAsset(userId)).subscribeWith(TestSubscriber()) }
            Messages.deleteUserMessages(userId)
            listOf(chat1Subscriber, chat2Subscriber).forEach {
                it.assertValues(UserChatMessagesRemoval(chat1Id, userId), UserChatMessagesRemoval(chat2Id, userId))
            }
        }
    }

    context("delete(Int)") {
        test("Deleting a message should delete it and its statuses") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val messageId = Messages.message(adminId, chatId, TextMessage("t"))
            MessageStatuses.create(userId, messageId, MessageStatus.DELIVERED)
            Messages.delete(messageId)
            Messages.readIdList(chatId).shouldBeEmpty()
            MessageStatuses.count().shouldBeZero()
        }

        test("Deleting a message should trigger a notification") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId, TextMessage("t"))
            val subscriber = messagesBroker.subscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            Messages.delete(messageId)
            subscriber.assertValue(DeletedMessage(chatId, messageId))
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
            val chatId = GroupChats.create(adminId)
            val now = LocalDateTime.now()
            Messages.create(adminId, chatId, TextMessage("t"))
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
            val chatId = GroupChats.create(adminId)
            Messages.create(adminId, chatId, TextMessage("t"))
            Messages.existsFrom(chatId, LocalDateTime.now()).shouldBeFalse()
        }
    }

    context("readPrivateChat(Int, String, BackwardPagination?)") {
        test("Messages deleted by the user via a private chat deletion should only be visible to the other user") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId, TextMessage("t"))
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(user1Id, chatId, TextMessage("t"))
            Messages.readPrivateChat(user1Id, chatId) shouldHaveSize 1
            Messages.readPrivateChat(user2Id, chatId) shouldHaveSize 2
        }
    }

    context("readChat(Int, BackwardPagination?, Op<Boolean>?)") {
        test("Messages should only be retrieved from the specified chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val create = {
                GroupChats.create(adminId).also { Messages.create(adminId, it, TextMessage("t")) }
            }
            create()
            Messages.readGroupChat(adminId, create()) shouldHaveSize 1
        }

        test("Messages should be retrieved in the order of their creation.") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val messagesIdList = (1..3).map { Messages.message(adminId, chatId, TextMessage("t")) }
            Messages.readGroupChat(adminId, chatId).map { it.cursor } shouldBe messagesIdList
        }

        test("Every message should be retrieved if neither the cursor nor the limit are supplied") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val messageIdList = (1..3).map { Messages.message(adminId, chatId, TextMessage("t")) }
            Messages.readGroupChat(adminId, chatId).map { it.cursor } shouldBe messageIdList
        }

        test(
            """
                Given both a limit and cursor,
                when retrieving messages,
                then the number of messages specified by the limit should be retrieved from before the cursor
                """
        ) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val messageIdList = (1..10).map { Messages.message(adminId, chatId, TextMessage("t")) }
            val last = 3
            val cursorIndex = 7
            Messages
                .readGroupChat(adminId, chatId, BackwardPagination(last, before = messageIdList[cursorIndex]))
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
            val chatId = GroupChats.create(adminId)
            val messageIdList = (1..5).map { Messages.message(adminId, chatId, TextMessage("t")) }
            val last = 3
            Messages.readGroupChat(adminId, chatId, BackwardPagination(last)).map { it.cursor } shouldBe
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
            val chatId = GroupChats.create(adminId)
            val messages = 5
            val messageIdList = (1..messages).map { Messages.message(adminId, chatId, TextMessage("t")) }
            val index = 3
            val cursor = messageIdList[index]
            Messages.readGroupChat(adminId, chatId, BackwardPagination(before = cursor)).map { it.cursor } shouldBe
                    messageIdList.dropLast(messages - index)
        }

        test("Using a deleted message's cursor shouldn't cause pagination to behave differently") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val messageIdList = (1..10).map { Messages.message(adminId, chatId, TextMessage("t")) }
            val index = 5
            val deletedMessageId = messageIdList[index]
            Messages.delete(deletedMessageId)
            val last = 3
            Messages
                .readGroupChat(adminId, chatId, BackwardPagination(last, before = deletedMessageId))
                .map { it.cursor } shouldBe messageIdList.subList(index - last, index)
        }
    }

    context("readPrivateChatConnection(Int, String, BackwardPagination?)") {
        test("Messages deleted via a private chat deletion shouldn't be retrieved only for the user who deleted it") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(user1Id, chatId, TextMessage("t"))
            PrivateChatDeletions.create(chatId, user1Id)
            val message2Id = Messages.message(user1Id, chatId, TextMessage("t"))
            val assert = { userId: Int, messageIdList: List<Int> ->
                Messages.readPrivateChatConnection(chatId, userId).edges.map { it.cursor } shouldBe messageIdList
            }
            assert(user1Id, listOf(message2Id))
            assert(user2Id, listOf(message1Id, message2Id))
        }
    }

    context("hasMessages(Int, Int, Chronology, Op<Boolean>?)") {
        data class CreatedChat(val adminId: Int, val chatId: Int, val firstMessageId: Int, val secondMessageId: Int)

        fun createChat(): CreatedChat {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val message = { Messages.message(adminId, chatId, TextMessage("t")) }
            return CreatedChat(adminId, chatId, firstMessageId = message(), secondMessageId = message())
        }

        test("There shouldn't be messages before the first message") {
            val (adminId, chatId, firstMessageId) = createChat()
            Messages.readGroupChatConnection(adminId, chatId, BackwardPagination(before = firstMessageId))
                .pageInfo
                .hasPreviousPage
                .shouldBeFalse()
        }

        test("There shouldn't be messages after the last message") {
            val (adminId, chatId, _, lastMessageId) = createChat()
            Messages.readGroupChatConnection(adminId, chatId, BackwardPagination(before = lastMessageId))
                .pageInfo
                .hasNextPage
                .shouldBeFalse()
        }

        test("There should be messages before the last message") {
            val (adminId, chatId, _, lastMessageId) = createChat()
            Messages.readGroupChatConnection(adminId, chatId, BackwardPagination(last = 0, before = lastMessageId))
                .pageInfo
                .hasPreviousPage
                .shouldBeTrue()
        }

        test("There should be messages after the first message") {
            val (adminId, chatId, firstMessageId) = createChat()
            Messages.readGroupChatConnection(adminId, chatId, BackwardPagination(before = firstMessageId))
                .pageInfo
                .hasNextPage
                .shouldBeTrue()
        }
    }

    context("readCursor(Int, CursorType, Filter)") {
        fun assertCursor(hasMessage: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val messageId = if (hasMessage) Messages.message(adminId, chatId, TextMessage("t")) else null
            with(Messages.readGroupChatConnection(adminId, chatId).pageInfo) {
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
            val chatId = GroupChats.create(adminId)
            val messageIdList = (1..5).map { Messages.message(adminId, chatId, TextMessage("t")) }
            with(Messages.readGroupChatConnection(adminId, chatId).pageInfo) {
                startCursor shouldBe messageIdList.first()
                endCursor shouldBe messageIdList.last()
            }
        }
    }
})
