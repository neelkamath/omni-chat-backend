package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.ExitedUser
import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.GroupChatInfoAsset
import com.neelkamath.omniChat.db.groupChatInfoBroker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.reactivex.rxjava3.subscribers.TestSubscriber

class GroupChatUsersTest : FunSpec({
    context("areInSameChat(String, String)") {
        test("Two users should be said to share a chat if they have at least one chat in common") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, buildNewGroupChat(userId))
            GroupChatUsers.areInSameChat(adminId, userId).shouldBeTrue()
            GroupChatUsers.areInSameChat(userId, adminId).shouldBeTrue()
        }

        test("Two users shouldn't be said to share a chat if they don't") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            GroupChatUsers.areInSameChat(user1Id, user2Id).shouldBeFalse()
            GroupChatUsers.areInSameChat(user2Id, user1Id).shouldBeFalse()
        }
    }

    context("addUsers(Int, Set<String>)") {
        test("Users should be added to the chat, ignoring the ones already in it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val initialUserIdList = listOf(user1Id)
            val chatId = GroupChats.create(adminId, buildNewGroupChat(initialUserIdList))
            val newUserIdList = listOf(user2Id)
            GroupChatUsers.addUsers(chatId, initialUserIdList + newUserIdList)
            GroupChatUsers.readUserIdList(chatId) shouldContainExactlyInAnyOrder
                    initialUserIdList + newUserIdList + adminId
        }
    }

    context("removeUsers(Int, List<String>)") {
        test("When a user leaves, a notification should be sent only to the other user in the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { groupChatInfoBroker.subscribe(GroupChatInfoAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.removeUsers(chatId, userId)
            adminSubscriber.assertValue(ExitedUser(chatId, userId))
            userSubscriber.assertNoValues()
        }
    }
})