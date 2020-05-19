package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.MessageStatuses
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.db.PrivateChats
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MessagesStatusesTest : FunSpec({
    listener(DbListener())

    context("create(Int, String, MessageStatus)") {
        test("Saving a duplicate message status should throw an exception") {
            val adminId = "admin ID ID"
            val userId = "user ID"
            val chat = NewGroupChat("Title", userIdList = setOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val messageId = Messages.message(chatId, adminId, "text")
            val createStatus = { MessageStatuses.create(messageId, userId, MessageStatus.DELIVERY) }
            createStatus()
            shouldThrowExactly<IllegalArgumentException>(createStatus)
        }

        test("""Recording a "read" status shouldn't create a "delivery" status if one was already recorded""") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERY)
            MessageStatuses.create(messageId, user2Id, MessageStatus.READ)
            MessageStatuses.count() shouldBe 2
        }

        test(""""Recording a "read" status should automatically record a "delivery" status if there wasn't one""") {
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
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERY)
            subscriber.assertValue(Messages.read(chatId)[0])
        }
    }
})