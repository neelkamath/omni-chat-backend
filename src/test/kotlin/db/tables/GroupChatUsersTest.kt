package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.GroupChatInfoAsset
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.groupChatInfoBroker
import com.neelkamath.omniChat.db.messagesBroker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.reactivex.rxjava3.subscribers.TestSubscriber

class GroupChatUsersTest : FunSpec({
    context("addUsers(Int, Set<String>)") {
        test("Users should be added to the chat, ignoring the ones already in it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val initialUserIdList = listOf(user1Id)
            val chatId = GroupChats.create(adminId, initialUserIdList)
            val newUserIdList = listOf(user2Id)
            GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
            GroupChatUsers.readUserIdList(chatId) shouldContainExactlyInAnyOrder
                    initialUserIdList + newUserIdList + adminId
        }
    }

    context("removeUsers(Int, List<String>)") {
        test("A user who leaves the chat should have their subscription removed") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            val subscriber = messagesBroker
                .subscribe(MessagesAsset(userId, chatId))
                .subscribeWith(TestSubscriber())
            GroupChatUsers.removeUsers(chatId, userId)
            subscriber.assertComplete()
        }

        test("Removed users should be unsubscribed only from the chat they were removed from") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(adminId, listOf(userId)) }
            val (chat1Subscriber, chat2Subscriber) = listOf(chat1Id, chat2Id).map {
                messagesBroker
                    .subscribe(MessagesAsset(userId, it))
                    .subscribeWith(TestSubscriber())
            }
            GroupChatUsers.removeUsers(chat1Id, userId)
            chat1Subscriber.assertComplete()
            chat2Subscriber.assertNotComplete()
        }

        test("Removed users should be unsubscribed from group chat updates") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            val subscriber = groupChatInfoBroker
                .subscribe(GroupChatInfoAsset(chatId, userId))
                .subscribeWith(TestSubscriber())
            GroupChatUsers.removeUsers(chatId, userId)
            subscriber.assertComplete()
        }
    }
})