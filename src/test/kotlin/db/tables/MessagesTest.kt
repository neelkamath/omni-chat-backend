package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.time.LocalDateTime

class MessagesTest : FunSpec({
    data class CreatedMessage(val creatorId: Int, val message: String)

    context("isInvalidBroadcast(Int, Int)") {
        test("Messaging in a private chat shouldn't count as an invalid broadcast") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.isInvalidBroadcast(user1Id, chatId).shouldBeFalse()
        }

        test("Admins and users messaging in non-broadcast group chats shouldn't be invalid broadcasts") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            Messages.isInvalidBroadcast(userId, chatId).shouldBeFalse()
            Messages.isInvalidBroadcast(adminId, chatId).shouldBeFalse()
        }

        test("Only an admin should be able to message in a broadcast group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(adminId, userId),
                adminIdList = listOf(adminId),
                isBroadcast = true
            )
            val chatId = GroupChats.create(chat)
            Messages.isInvalidBroadcast(adminId, chatId).shouldBeFalse()
            Messages.isInvalidBroadcast(userId, chatId).shouldBeTrue()
        }
    }

    context("isVisible(Int, String)") {
        test("A nonexistent message shouldn't be said to be visible") {
            val userId = createVerifiedUsers(1)[0].info.id
            Messages.isVisible(userId, messageId = 1).shouldBeFalse()
        }

        test("The message shouldn't be visible if the user isn't in the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Messages.isVisible(userId, messageId).shouldBeFalse()
        }

        test("The message should be visible if the user is in the group chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Messages.isVisible(adminId, messageId).shouldBeTrue()
        }

        fun createChatWithMessage(shouldDelete: Boolean) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            if (shouldDelete) PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId)
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
            val messageId = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.isVisible(user1Id, messageId).shouldBeFalse()
        }
    }

    context("search(List<MessageEdge>, String)") {
        test("Text messages should be searched case insensitively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId, MessageText("Hi"))
            Messages.message(adminId, chatId, MessageText("Bye"))
            Messages.searchGroupChat(adminId, chatId, "hi").map { it.node.messageId } shouldBe listOf(messageId)
        }

        test("Pic message captions should be searched case insensitively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            val message1 = CaptionedPic(pic, caption = MessageText("Hi"))
            val messageId = Messages.message(adminId, chatId, message1)
            val message2 = CaptionedPic(pic, caption = MessageText("Bye"))
            Messages.message(adminId, chatId, message2)
            Messages.searchGroupChat(adminId, chatId, "hi").map { it.node.messageId } shouldBe listOf(messageId)
        }

        test("Poll message title and options should be searched case insensitively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message1Options = listOf(MessageText("Burger King"), MessageText("Pizza Hut"))
            val message1 = PollInput(title = MessageText("Restaurant"), options = message1Options)
            val message1Id = Messages.message(adminId, chatId, message1)
            val message2Options = listOf(MessageText("Japanese Restaurant"), MessageText("Thai Restaraunt"))
            val message2 = PollInput(MessageText("Title"), message2Options)
            val message2Id = Messages.message(adminId, chatId, message2)
            val message3Options = listOf(MessageText("option 1"), MessageText("option 2"))
            val message3 = PollInput(MessageText("Title"), message3Options)
            Messages.message(adminId, chatId, message3)
            Messages.searchGroupChat(adminId, chatId, "restaurant").map { it.node.messageId } shouldBe
                    listOf(message1Id, message2Id)
        }

        test("Audio messages shouldn't be returned") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            Messages.message(adminId, chatId, Mp3(ByteArray(1)))
            Messages.searchGroupChat(adminId, chatId, "query").shouldBeEmpty()
        }
    }

    context("create(Int, Int, MessageType, Int?, (Int) -> Unit)") {
        test("Subscribers should receive notifications of created messages") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id))
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            repeat(3) { Messages.create(listOf(adminId, user1Id, user2Id).random(), chatId) }
            awaitBrokering()
            mapOf(adminId to adminSubscriber, user1Id to user1Subscriber, user2Id to user2Subscriber)
                .forEach { (userId, subscriber) ->
                    val updates = Messages.readGroupChat(userId, chatId).map { it.node.toNewTextMessage() }
                    subscriber.assertValueSequence(updates)
                }
        }

        test("A subscriber should be notified of a new message in a private chat they just deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            val messageId = Messages.message(user2Id, chatId)
            awaitBrokering()
            Messages.readMessage(user1Id, messageId).toNewTextMessage().let(subscriber::assertValue)
        }

        test("An exception should be thrown if the user isn't in the chat") {
            val userId = createVerifiedUsers(1)[0].info.id
            shouldThrowExactly<IllegalArgumentException> {
                Messages.create(userId, chatId = 1, text = MessageText("t"))
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
            createdMessages.forEach { Messages.create(it.creatorId, chatId, MessageText(it.message)) }
            Messages.readPrivateChat(user1Id, chatId).forEachIndexed { index, message ->
                message.node.sender.id shouldBe createdMessages[index].creatorId
                message.node.messageId.let(TextMessages::read).value shouldBe createdMessages[index].message
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
            Messages.create(user1Id, chatId)
            Messages.create(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(user2Id, chatId)
            Messages.readPrivateChat(user1Id, chatId).map { it.node } shouldBe
                    listOf(Messages.readMessage(user1Id, messageId))
        }
    }

    context("deleteChatMessages(List<Int>)") {
        test("Deleting a chat containing message contexts should be deleted successfully") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextId = Messages.message(adminId, chatId)
            Messages.create(adminId, chatId, MessageText("t"), contextId)
            shouldNotThrowAny { Messages.deleteChat(chatId) }
        }

        test(
            """
            Given message 1 from user 1, and message 2 from user 2 which has message 1 as its context,
            when user 1 deletes their messages,
            then message 2's context should be set to null
            """
        ) {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val contextId = Messages.message(userId, chatId)
            val messageId = Messages.message(adminId, chatId, contextMessageId = contextId)
            Messages.deleteUserChatMessages(chatId, userId)
            Messages.readMessage(adminId, messageId).context.id.shouldBeNull()
        }
    }

    context("deleteChat(Int)") {
        test("The subscriber should be notified of every message being deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            Messages.deleteChat(chatId)
            awaitBrokering()
            subscriber.assertValue(DeletionOfEveryMessage(chatId))
        }
    }

    context("deleteChatUntil(Int, LocalDateTime)") {
        test("Every message should be deleted until the specified point") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            Messages.create(adminId, chatId)
            val now = LocalDateTime.now()
            val messageId = Messages.message(adminId, chatId)
            Messages.deleteChatUntil(chatId, now)
            Messages.readIdList(chatId) shouldBe listOf(messageId)
        }

        test("Subscribers should be notified of the message deletion point") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
            val until = LocalDateTime.now()
            Messages.deleteChatUntil(chatId, until)
            awaitBrokering()
            subscriber.assertValue(MessageDeletionPoint(chatId, until))
        }
    }

    context("deleteUserChatMessages(Int, String)") {
        test("A subscriber should be notified when a user's messages have been deleted from the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
            Messages.deleteUserChatMessages(chatId, userId)
            awaitBrokering()
            subscriber.assertValue(UserChatMessagesRemoval(chatId, userId))
        }
    }

    context("deleteUserMessages(String)") {
        test("Subscribers should be notified when every message the user sent is deleted") {
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

    context("delete(Int)") {
        test("Deleting a message should trigger a notification") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            Messages.delete(messageId)
            awaitBrokering()
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
            val chatId = GroupChats.create(listOf(adminId))
            val now = LocalDateTime.now()
            Messages.create(adminId, chatId)
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
            val chatId = GroupChats.create(listOf(adminId))
            Messages.create(adminId, chatId)
            Messages.existsFrom(chatId, LocalDateTime.now()).shouldBeFalse()
        }
    }

    context("readPrivateChat(Int, String, BackwardPagination?)") {
        test("Messages deleted by the user via a private chat deletion should only be visible to the other user") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(user1Id, chatId)
            Messages.readPrivateChat(user1Id, chatId) shouldHaveSize 1
            Messages.readPrivateChat(user2Id, chatId) shouldHaveSize 2
        }
    }

    context("readChat(Int, BackwardPagination?, Op<Boolean>?)") {
        test("Messages should only be retrieved from the specified chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val create = {
                GroupChats.create(listOf(adminId)).also { Messages.create(adminId, it) }
            }
            create()
            Messages.readGroupChat(adminId, create()) shouldHaveSize 1
        }

        test("Messages should be retrieved in the order of their creation.") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messagesIdList = (1..3).map { Messages.message(adminId, chatId) }
            Messages.readGroupChat(adminId, chatId).map { it.cursor } shouldBe messagesIdList
        }

        test("Every message should be retrieved if neither the cursor nor the limit are supplied") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..3).map { Messages.message(adminId, chatId) }
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
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
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
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..5).map { Messages.message(adminId, chatId) }
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
            val chatId = GroupChats.create(listOf(adminId))
            val messages = 5
            val messageIdList = (1..messages).map { Messages.message(adminId, chatId) }
            val index = 3
            val cursor = messageIdList[index]
            Messages.readGroupChat(adminId, chatId, BackwardPagination(before = cursor)).map { it.cursor } shouldBe
                    messageIdList.dropLast(messages - index)
        }

        test("Using a deleted message's cursor shouldn't cause pagination to behave differently") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
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
            val message1Id = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message2Id = Messages.message(user1Id, chatId)
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
            val chatId = GroupChats.create(listOf(adminId))
            val message = { Messages.message(adminId, chatId) }
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
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = if (hasMessage) Messages.message(adminId, chatId) else null
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
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..5).map { Messages.message(adminId, chatId) }
            with(Messages.readGroupChatConnection(adminId, chatId).pageInfo) {
                startCursor shouldBe messageIdList.first()
                endCursor shouldBe messageIdList.last()
            }
        }
    }
})
