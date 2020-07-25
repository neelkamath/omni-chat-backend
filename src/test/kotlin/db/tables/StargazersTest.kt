package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.UpdatedMessage
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.messagesBroker
import io.kotest.core.spec.style.FunSpec
import io.reactivex.rxjava3.subscribers.TestSubscriber

class StargazersTest : FunSpec({
    context("create(Int, Int)") {
        test("Starring should only notify the stargazer") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                .map { messagesBroker.subscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            Stargazers.create(user1Id, messageId)
            user1Subscriber.assertValue(UpdatedMessage.build(user1Id, messageId))
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
                .map { messagesBroker.subscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            Stargazers.deleteUserStar(user1Id, messageId)
            user1Subscriber.assertValue(UpdatedMessage.build(user1Id, messageId))
            user2Subscriber.assertNoValues()
        }

        test("Deleting a nonexistent star shouldn't cause anything to happen") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val subscriber = messagesBroker.subscribe(MessagesAsset(adminId)).subscribeWith(TestSubscriber())
            Stargazers.deleteUserStar(adminId, messageId)
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
                .map { messagesBroker.subscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            Stargazers.deleteStar(messageId)
            mapOf(adminId to adminSubscriber, user1Id to user1Subscriber)
                .forEach { (userId, subscriber) -> subscriber.assertValue(UpdatedMessage.build(userId, messageId)) }
            user2Subscriber.assertNoValues()
        }
    }
})