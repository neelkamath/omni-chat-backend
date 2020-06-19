package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.db.MessageStatuses
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.db.unsubscribeUserFromMessageUpdates
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec

class MessageUpdatesTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    context("subscribeToMessageUpdates(String, Int)") {
        test("Notifications for message updates made before subscribing shouldn't be sent") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(chatId, user1Id, "Hello")
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            Messages.create(chatId, user2Id, "Hi")
            Messages.create(chatId, user1Id, "How are you?")
            val messages = Messages.readPrivateChat(chatId, user1Id).drop(1).map { it.node }.toTypedArray()
            subscriber.assertValues(*messages)
        }
    }

    context("unsubscribeFromMessageUpdates(String, Int)") {
        test("Unsubscribing should stop notifications") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            unsubscribeUserFromMessageUpdates(user1Id, chatId)
            subscriber.assertComplete()
        }
    }

    context("notifyMessageUpdate(Int)") {
        test("The subscriber should be notified of the updated message") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            val subscriber = createMessageUpdatesSubscriber(user1Id, chatId)
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            val message = Messages.readPrivateChat(chatId, user1Id)[0].node
            subscriber.assertValue(message)
        }
    }
}