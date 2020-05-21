package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.MessageStatuses
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.db.PrivateChats
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class MessageStatusesTest : FunSpec({
    listener(DbListener())

    context("create(Int, String, MessageStatus)") {
        test("Saving a duplicate message status should throw an exception") {
            val adminId = "admin ID ID"
            val userId = "user ID"
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val messageId = Messages.message(chatId, adminId, "text")
            val createStatus = { MessageStatuses.create(messageId, userId, MessageStatus.DELIVERED) }
            createStatus()
            shouldThrowExactly<IllegalArgumentException>(createStatus)
        }

        test("""Recording a "read" status shouldn't create a "delivered" status if one was already recorded""") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            MessageStatuses.create(messageId, user2Id, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test(""""Recording a "read" status should automatically record a "delivered" status if there wasn't one""") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(messageId, user2Id, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test("Creating a status for the user on their own message should throw an exception") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            shouldThrowExactly<IllegalArgumentException> {
                MessageStatuses.create(
                    messageId,
                    user1Id,
                    MessageStatus.READ
                )
            }
        }
    }

    context("insertAndNotify(Int, String, MessageStatus)") {
        test("A subscriber should be notified of updated statuses") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val text = "text"
            val messageId = Messages.message(chatId, user1Id, text)
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            subscriber.assertValue(Messages.readChat(chatId)[0])
        }
    }

    context("delete(Int, String)") {
        /**
         * Creates a private chat between [user1Id] and [user2Id], has [user2Id] send a message in it, has [user1Id]
         * create a [MessageStatus.DELIVERED] on it, and returns the chat's ID.
         */
        fun createUsedChat(user1Id: String, user2Id: String): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user2Id, "text")
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
            val (user1Id, user2Id, user3Id) = (1..3).map { "user $it ID" }
            val chat1Id = createUsedChat(user1Id, user2Id)
            val chat2Id = createUsedChat(user1Id, user3Id)
            MessageStatuses.delete(chat1Id, user1Id)
            Messages.readChat(chat1Id).flatMap { it.dateTimes.statuses }.shouldBeEmpty()
            Messages.readChat(chat2Id).flatMap { it.dateTimes.statuses }.shouldNotBeEmpty()
        }
    }
})