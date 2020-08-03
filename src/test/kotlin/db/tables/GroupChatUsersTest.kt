package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.ExitedUser
import com.neelkamath.omniChat.graphql.routing.GroupChatId
import com.neelkamath.omniChat.graphql.routing.UpdatedGroupChat
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class GroupChatUsersTest : FunSpec({
    context("readFellowParticipants(Int)") {
        test("Reading fellow participants should only include other participants in the chats") {
            val (adminId, participantId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(participantId))
            GroupChatUsers.readFellowParticipants(adminId) shouldBe setOf(participantId)
        }
    }

    context("addUsers(Int, List<String>)") {
        test("Users should be added to the chat while ignoring duplicates and participants") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id))
            GroupChatUsers.addUsers(chatId, user1Id, user2Id, user2Id)
            GroupChatUsers.readUserIdList(chatId) shouldContainExactlyInAnyOrder listOf(adminId, user1Id, user2Id)
        }

        test("Only added users should be notified of the new group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { newGroupChatsBroker.subscribe(NewGroupChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.addUsers(chatId, userId)
            adminSubscriber.assertNoValues()
            userSubscriber.assertValue(GroupChatId(chatId))
        }

        test("Only existing participants should get notified when users are added") {
            val (admin, user) = createVerifiedUsers(2).map { it.info }
            val chatId = GroupChats.create(listOf(admin.id))
            val (adminSubscriber, userSubscriber) = listOf(admin.id, user.id)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.addUsers(chatId, user.id)
            val update = UpdatedGroupChat(chatId, newUsers = listOf(user))
            adminSubscriber.assertValue(update)
            userSubscriber.assertNoValues()
        }
    }

    context("canUserLeave(Int)") {
        test("The user should be able to leave if they aren't the last admin of a chat with other users in it") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.canUserLeave(userId).shouldBeTrue()
        }

        test("The user shouldn't be able to leave if they're the last admin of a chat with other users in it") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.canUserLeave(adminId).shouldBeFalse()
        }
    }

    context("canUsersLeave(Int, Set<Int>)") {
        test("""Checking if users who aren't in the chat can leave should return "false"""") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            GroupChatUsers.canUsersLeave(chatId, userId).shouldBeFalse()
        }

        test("The users should be able to leave if the chat will be empty") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.canUsersLeave(chatId, adminId, userId).shouldBeTrue()
        }

        test("The users shouldn't be able to leave if the chat will have users sans admins") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.canUsersLeave(chatId, adminId).shouldBeFalse()
        }

        test("A non-admin should be able to leave if the chat will have admins remaining") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.canUsersLeave(chatId, userId).shouldBeTrue()
        }

        test("An admin should be able to leave if the chat has another admin") {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id, admin2Id))
            GroupChatUsers.canUsersLeave(chatId, admin1Id).shouldBeTrue()
        }
    }

    context("makeAdmins(Int, List<String>)") {
        test("Setting the admin to a user who isn't in the chat should throw an exception") {
            val (adminId, invalidAdminId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            shouldThrowExactly<IllegalArgumentException> { GroupChatUsers.makeAdmins(chatId, invalidAdminId) }
        }

        test("Only the specified users should be made admins") {
            val (adminId, user1Id, user2Id, user3Id) = createVerifiedUsers(4).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id, user3Id))
            GroupChatUsers.makeAdmins(chatId, user1Id, user2Id)
            GroupChatUsers.isAdmin(user1Id, chatId).shouldBeTrue()
            GroupChatUsers.isAdmin(user2Id, chatId).shouldBeTrue()
            GroupChatUsers.isAdmin(user3Id, chatId).shouldBeFalse()
        }

        test("Making admins should only notify participants") {
            val (adminId, toBeAdminId, nonParticipantId) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), userIdList = listOf(toBeAdminId))
            val (adminSubscriber, toBeAdminSubscriber, nonParticipantSubscriber) =
                listOf(adminId, toBeAdminId, nonParticipantId)
                    .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.makeAdmins(chatId, toBeAdminId)
            listOf(adminSubscriber, toBeAdminSubscriber)
                .forEach { it.assertValue(UpdatedGroupChat(chatId, adminIdList = listOf(adminId, toBeAdminId))) }
            nonParticipantSubscriber.assertNoValues()
        }
    }

    context("removeUsers(Int, List<Int>)") {
        test("An exception should be thrown if the users can't leave the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            shouldThrowExactly<IllegalArgumentException> { GroupChatUsers.removeUsers(chatId, adminId) }
        }

        test("""Users should be removed from the chat returning "false"""") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.removeUsers(chatId, userId).shouldBeFalse()
            GroupChatUsers.readUserIdList(chatId) shouldBe listOf(adminId)
        }

        test("Participants should be notified of removed users once even if the user was removed twice") {
            val (adminId, userId, nonParticipantId) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val (adminSubscriber, userSubscriber, nonParticipantSubscriber) = listOf(adminId, userId, nonParticipantId)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.removeUsers(chatId, userId, userId)
            adminSubscriber.assertValue(ExitedUser(userId, chatId))
            listOf(userSubscriber, nonParticipantSubscriber).forEach { it.assertNoValues() }
        }

        test("""Removing every user should delete the chat returning "true"""") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.removeUsers(chatId, adminId, userId).shouldBeTrue()
            GroupChats.count().shouldBeZero()
        }
    }

    context("isAdmin(Int, Int)") {
        test("A non-admin participating in the chat shouldn't be said to be an admin") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.isAdmin(userId, chatId).shouldBeFalse()
        }

        test("A non-participant shouldn't be said to be an admin") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            GroupChatUsers.isAdmin(userId, chatId).shouldBeFalse()
        }

        test("The admin of a chat should have such stated") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            GroupChatUsers.isAdmin(adminId, chatId).shouldBeTrue()
        }
    }
})