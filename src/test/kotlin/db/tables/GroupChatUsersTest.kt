package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.ExitedUser
import com.neelkamath.omniChat.buildNewGroupChat
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.UpdatedChatsAsset
import com.neelkamath.omniChat.db.updatedChatsBroker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class GroupChatUsersTest : FunSpec({
    context("readFellowParticipants(Int)") {
        test("Reading fellow participants should only include other participants in the chats") {
            val (adminId, participantId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, buildNewGroupChat(participantId))
            GroupChatUsers.readFellowParticipants(adminId) shouldBe setOf(participantId)
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
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.removeUsers(chatId, userId)
            adminSubscriber.assertValue(ExitedUser(chatId, userId))
            userSubscriber.assertNoValues()
        }
    }
})