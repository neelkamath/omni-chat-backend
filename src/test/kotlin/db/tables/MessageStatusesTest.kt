package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.messagesBroker
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
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val messageId = Messages.message(chatId, adminId, TextMessage("t"))
            val createStatus = { MessageStatuses.create(messageId, userId, MessageStatus.DELIVERED) }
            createStatus()
            shouldThrowExactly<IllegalArgumentException>(createStatus)
        }

        test("""Recording a "read" status shouldn't create a "delivered" status if one was already recorded""") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            MessageStatuses.create(messageId, user2Id, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test(""""Recording a "read" status should automatically record a "delivered" status if there wasn't one""") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
            MessageStatuses.create(messageId, user2Id, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test("Creating a status for the user on their own message should throw an exception") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
            shouldThrowExactly<IllegalArgumentException> {
                MessageStatuses.create(messageId, user1Id, MessageStatus.READ)
            }
        }

        test("The user shouldn't be able to create a status on a message sent before they deleted the chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user2Id, TextMessage("t"))
            PrivateChatDeletions.create(chatId, user1Id)
            shouldThrowExactly<IllegalArgumentException> {
                MessageStatuses.create(messageId, user1Id, MessageStatus.READ)
            }
        }
    }

    context("insertAndNotify(Int, String, MessageStatus)") {
        test("A subscriber should be notified of updated statuses") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
            val subscriber = messagesBroker.subscribe(MessagesAsset(user1Id)).subscribeWith(TestSubscriber())
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            val message = Messages.readPrivateChat(chatId, user1Id)[0].node
            subscriber.assertValue(UpdatedMessage.build(chatId, message))
        }
    }

    context("delete(Int, String)") {
        /**
         * Creates a private chat between [user1Id] and [user2Id], has [user2Id] send a message in it, has [user1Id]
         * create a [MessageStatus.DELIVERED] on it, and returns the chat's ID.
         */
        fun createUsedChat(user1Id: String, user2Id: String): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user2Id, TextMessage("t"))
            MessageStatuses.create(messageId, user1Id, MessageStatus.DELIVERED)
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
            Messages.readPrivateChat(chat1Id, user1Id).flatMap { it.node.dateTimes.statuses }.shouldBeEmpty()
            Messages.readPrivateChat(chat2Id, user1Id).flatMap { it.node.dateTimes.statuses }.shouldNotBeEmpty()
        }
    }
})