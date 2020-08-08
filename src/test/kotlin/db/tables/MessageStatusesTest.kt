package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.MessageStatus
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class MessageStatusesTest : FunSpec({
    context("create(Int, String, MessageStatus)") {
        test("Saving a duplicate message status should throw an exception") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            val createStatus = { MessageStatuses.create(userId, messageId, MessageStatus.DELIVERED) }
            createStatus()
            shouldThrowExactly<IllegalArgumentException>(createStatus)
        }

        test("""Recording a "read" status shouldn't create a "delivered" status if one was already recorded""") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
            MessageStatuses.create(user2Id, messageId, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test(""""Recording a "read" status should automatically record a "delivered" status if there wasn't one""") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            MessageStatuses.create(user2Id, messageId, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test("Creating a status for the user on their own message should throw an exception") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            shouldThrowExactly<IllegalArgumentException> {
                MessageStatuses.create(user1Id, messageId, MessageStatus.READ)
            }
        }

        test("The user shouldn't be able to create a status on a message sent before they deleted the chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            shouldThrowExactly<IllegalArgumentException> {
                MessageStatuses.create(user1Id, messageId, MessageStatus.READ)
            }
        }
    }

    context("insertAndNotify(Int, String, MessageStatus)") {
        test("Only subscribers in the chat should be notified of updated statuses") {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChats.create(user2Id, user3Id)
            val messageId = Messages.message(user1Id, chatId)
            val (user1Subscriber, user2Subscriber, user3Subscriber) = listOf(user1Id, user2Id, user3Id)
                .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
            awaitBrokering()
            mapOf(user1Subscriber to user1Id, user2Subscriber to user2Id).forEach { (subscriber, userId) ->
                Messages.readMessage(userId, messageId).toUpdatedTextMessage().let(subscriber::assertValue)
            }
            user3Subscriber.assertNoValues()
        }
    }

    context("delete(Int, String)") {
        /**
         * Creates a private chat between [user1Id] and [user2Id], has [user2Id] send a message in it, has [user1Id]
         * create a [MessageStatus.DELIVERED] on it, and returns the chat's ID.
         */
        fun createUsedChat(user1Id: Int, user2Id: Int): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            MessageStatuses.create(user1Id, messageId, MessageStatus.DELIVERED)
            return chatId
        }

        test(
            """
            Given a user in two chats,
            when deleting the user's statuses in one of the chats,
            then only the statuses the user created in that chat should be deleted
            """
        ) {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chat1Id = createUsedChat(user1Id, user2Id)
            val chat2Id = createUsedChat(user1Id, user3Id)
            MessageStatuses.deleteUserChatStatuses(chat1Id, user1Id)
            Messages.readPrivateChat(user1Id, chat1Id).flatMap { it.node.dateTimes.statuses }.shouldBeEmpty()
            Messages.readPrivateChat(user1Id, chat2Id).flatMap { it.node.dateTimes.statuses }.shouldNotBeEmpty()
        }
    }
})