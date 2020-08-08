package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.subscribers.TestSubscriber

class StargazersTest : FunSpec({
    context("create(Int, Int)") {
        test("Starring should only notify the stargazer") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            Stargazers.create(user1Id, messageId)
            awaitBrokering()
            user1Subscriber.assertValue(Messages.readMessage(user1Id, messageId).toUpdatedTextMessage())
            user2Subscriber.assertNoValues()
        }
    }

    context("deleteUserStar(Int, Int)") {
        test("Deleting a star should only notify the deleter") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            Stargazers.create(user1Id, messageId)
            val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            Stargazers.deleteUserStar(user1Id, messageId)
            awaitBrokering()
            user1Subscriber.assertValue(Messages.readMessage(user1Id, messageId).toUpdatedTextMessage())
            user2Subscriber.assertNoValues()
        }

        test("Deleting a nonexistent star shouldn't cause anything to happen") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val subscriber = messagesNotifier.safelySubscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
            Stargazers.deleteUserStar(adminId, messageId)
            awaitBrokering()
            subscriber.assertNoValues()
        }
    }

    context("deleteStar(Int)") {
        test("Deleting a message's stars should only notify its stargazers") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id))
            val messageId = Messages.message(adminId, chatId)
            listOf(adminId, user1Id).forEach { Stargazers.create(it, messageId) }
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            Stargazers.deleteStar(messageId)
            awaitBrokering()
            mapOf(adminId to adminSubscriber, user1Id to user1Subscriber).forEach { (userId, subscriber) ->
                subscriber.assertValue(Messages.readMessage(userId, messageId).toUpdatedTextMessage())
            }
            user2Subscriber.assertNoValues()
        }
    }
})