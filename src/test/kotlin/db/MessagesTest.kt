package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber
import java.time.LocalDateTime

class MessagesTest : FunSpec({
    listener(DbListener())

    data class CreatedMessage(val creator: String, val message: String)

    context("create(Int, String, String)") {
        test("Subscribers should receive notifications of created messages") {
            val adminId = "admin ID"
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chat = NewGroupChat("Title", userIdList = setOf(user1Id, user2Id))
            val chatId = GroupChats.create(adminId, chat)
            val adminSubscriber = createMessageUpdatesSubscriber(adminId, chatId)
            val user1Subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            val user2Subscriber = createMessageUpdatesSubscriber(user2Id, chatId)
            repeat(3) { Messages.create(chatId, listOf(adminId, user1Id, user2Id).random(), "text") }
            val updates = Messages.readChat(chatId).toTypedArray()
            listOf(adminSubscriber, user1Subscriber, user2Subscriber).forEach { it.assertValues(*updates) }
        }
    }

    context("read(Int)") {
        test("Messages should be read in the order of their creation") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val createdMessages = listOf(
                CreatedMessage(user1Id, "Hey"),
                CreatedMessage(user2Id, "Hi!"),
                CreatedMessage(user1Id, "I have a question"),
                CreatedMessage(user1Id, "Is tomorrow a holiday?")
            )
            createdMessages.forEach { Messages.create(chatId, it.creator, it.message) }
            Messages.readChat(chatId).forEachIndexed { index, message ->
                message.senderId shouldBe createdMessages[index].creator
                message.text shouldBe createdMessages[index].message
            }
        }

        test(
            """
            Given a chat deleted by the user which had activity after its deletion,
            when the chat is retrieved for the user,
            then it should exclude messages sent before its deletion
            """
        ) {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.message(chatId, user1Id, "text")
            Messages.message(chatId, user2Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            val messageId = Messages.message(chatId, user2Id, "text")
            Messages.read(chatId, user1Id) shouldBe listOf(Messages.read(messageId))
        }
    }

    context("deleteChat(Int)") {
        test("Deleting a chat should delete its messages and messages statuses") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            Messages.deleteChat(chatId)
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }

        test("The subscriber should be notified of every message being deleted, and then be unsubscribed") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            Messages.deleteChat(chatId)
            subscriber.assertValue(DeletionOfEveryMessage())
            subscriber.assertComplete()
        }
    }

    context("delete(Int, LocalDateTime)") {
        test("Every message and message status should be deleted until the specified point only") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val message1Id = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(message1Id, user2Id, MessageStatus.READ)
            val now = LocalDateTime.now()
            val message2Id = Messages.message(chatId, user2Id, "text")
            MessageStatuses.create(message2Id, user1Id, MessageStatus.DELIVERED)
            Messages.delete(chatId, until = now)
            Messages.readChat(chatId).map { it.id } shouldBe listOf(message2Id)
            MessageStatuses.count() shouldBe 1
        }
    }

    context("delete(Int, String)") {
        test("Every message and message status the user has in the chat should be deleted") {
            val (adminId, userId) = (1..2).map { "user $it ID" }
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val message1Id = Messages.message(chatId, adminId, "text")
            MessageStatuses.create(message1Id, userId, MessageStatus.READ)
            val message2Id = Messages.message(chatId, userId, "text")
            MessageStatuses.create(message2Id, adminId, MessageStatus.READ)
            Messages.delete(chatId, userId)
            Messages.readChat(chatId).map { it.id } shouldBe listOf(message1Id)
            MessageStatuses.count().shouldBeZero()
        }

        test("A subscriber should be notified when a user's messages have been deleted from the chat") {
            val (adminId, userId) = (1..2).map { "user $it ID" }
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val subscriber = createMessageUpdatesSubscriber(adminId, chatId)
            Messages.delete(chatId, userId)
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
            val chat = NewGroupChat("Title", userIdList = setOf(user2Id))
            val chatId = GroupChats.create(user1Id, chat)
            val message1Id = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(message1Id, user2Id, MessageStatus.READ)
            val message2Id = Messages.message(chatId, user2Id, "text")
            MessageStatuses.create(message2Id, user1Id, MessageStatus.READ)
            GroupChatUsers.removeUsers(chatId, setOf(user2Id))
            return chatId
        }

        test(
            """
            Given a user in a private chat, and a group chat they've left,
            when deleting the user's messages,
            then their messages, created message statues, and received message statuses should be deleted
            """
        ) {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val privateChatId = createUtilizedPrivateChat(user1Id, user2Id)
            val groupChatId = createUtilizedGroupChat(user1Id, user2Id)
            Messages.delete(user2Id)
            val testMessages = { chatId: Int ->
                val messages = Messages.readChat(chatId)
                messages.map { it.senderId } shouldBe listOf(user1Id)
                messages.flatMap { it.dateTimes.statuses }.map { it.userId }.shouldBeEmpty()
            }
            testMessages(privateChatId)
            testMessages(groupChatId)
        }

        /**
         * Creates a group chat with the [adminId] and [userId], has the [userId] send a message in it, and returns the
         * [userId]'s [TestSubscriber] to the chat.
         */
        fun createChatMessageSubscriber(adminId: String, userId: String): TestSubscriber<MessageUpdate> {
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            Messages.create(chatId, userId, "text")
            return createMessageUpdatesSubscriber(userId, chatId)
        }

        test("Subscribers should be notified when every message the user sent is deleted") {
            val (adminId, userId) = (1..2).map { "user $it ID" }
            val chat1Subscriber = createChatMessageSubscriber(adminId, userId)
            val chat2Subscriber = createChatMessageSubscriber(adminId, userId)
            Messages.delete(userId)
            val removal = UserChatMessagesRemoval(userId)
            chat1Subscriber.assertValue(removal)
            chat2Subscriber.assertValue(removal)
        }
    }

    context("delete(Int)") {
        test("Deleting a message should delete it and its statuses") {
            val (adminId, userId) = (1..2).map { "user $it ID" }
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val messageId = Messages.message(chatId, adminId, "text")
            MessageStatuses.create(messageId, userId, MessageStatus.DELIVERED)
            Messages.delete(messageId)
            Messages.readChat(chatId).size.shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }

        test("Deleting a message should trigger a notification") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
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
            val adminId = "user ID"
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
            val adminId = "user ID"
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            Messages.create(chatId, adminId, "text")
            Messages.existsFrom(chatId, LocalDateTime.now()).shouldBeFalse()
        }
    }
})