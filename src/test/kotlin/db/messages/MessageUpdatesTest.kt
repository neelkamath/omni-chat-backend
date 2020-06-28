package com.neelkamath.omniChat.db.messages

import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.chats.PrivateChats
import com.neelkamath.omniChat.toNewMessage
import com.neelkamath.omniChat.toUpdatedMessage
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.subscribers.TestSubscriber

class MessageUpdatesTest : FunSpec({
    context("subscribeToMessageUpdates(String, Int)") {
        test("Notifications for message updates made before subscribing shouldn't be sent") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(chatId, user1Id, "Hello")
            val subscriber = subscribeToMessageUpdates(user1Id, chatId).subscribeWith(TestSubscriber())
            Messages.create(chatId, user2Id, "Hi")
            Messages.create(chatId, user1Id, "How are you?")
            val messages = Messages.readPrivateChat(chatId, user1Id).drop(1).map { it.node.toNewMessage() }
            subscriber.assertValueSequence(messages)
        }
    }

    context("unsubscribeFromMessageUpdates(String, Int)") {
        test("Unsubscribing should stop notifications") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val subscriber = subscribeToMessageUpdates(user1Id, chatId).subscribeWith(TestSubscriber())
            unsubscribeUserFromMessageUpdates(user1Id, chatId)
            subscriber.assertComplete()
        }
    }

    context("notifyMessageUpdate(Int)") {
        test("The subscriber should be notified of the updated message") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            val subscriber = subscribeToMessageUpdates(user1Id, chatId).subscribeWith(TestSubscriber())
            MessageStatuses.create(messageId, user2Id, MessageStatus.DELIVERED)
            val message = Messages.readPrivateChat(chatId, user1Id)[0].node.toUpdatedMessage()
            subscriber.assertValue(message)
        }
    }
})