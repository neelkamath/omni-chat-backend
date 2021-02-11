package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.groupChatsNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.ExitedUser
import com.neelkamath.omniChat.graphql.routing.GroupChatId
import com.neelkamath.omniChat.graphql.routing.UpdatedGroupChat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class GroupChatUsersTest {
    @Nested
    inner class ReadFellowParticipants {
        @Test
        fun `Reading fellow participants must only include other participants in the chats`() {
            val (adminId, participantId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(participantId))
            assertEquals(setOf(participantId), GroupChatUsers.readFellowParticipants(adminId))
        }
    }

    @Nested
    inner class AddUsers {
        @Test
        fun `Users must be added to the chat while ignoring duplicates and participants`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id))
            GroupChatUsers.addUsers(chatId, user1Id, user2Id, user2Id)
            assertEquals(setOf(adminId, user1Id, user2Id), GroupChatUsers.readUserIdList(chatId).toSet())
        }

        @Test
        fun `New users must be notified of the chat, and existing participants of their addition`() {
            runBlocking {
                val (admin, user) = createVerifiedUsers(2).map { it.info }
                val chatId = GroupChats.create(listOf(admin.id))
                val (adminSubscriber, userSubscriber) =
                    listOf(admin.id, user.id).map { groupChatsNotifier.safelySubscribe(it) }
                GroupChatUsers.addUsers(chatId, user.id)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedGroupChat(chatId, newUsers = listOf(user)))
                userSubscriber.assertValue(GroupChatId(chatId))
            }
        }
    }

    @Nested
    inner class CanUserLeave {
        @Test
        fun `The user must be able to leave if they aren't the last admin of a chat with other users in it`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertTrue(GroupChatUsers.canUserLeave(userId))
        }

        @Test
        fun `The user mustn't be able to leave if they're the last admin of a chat with other users in it`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertFalse(GroupChatUsers.canUserLeave(adminId))
        }
    }

    @Nested
    inner class CanUsersLeave {
        @Test
        fun `Checking if users who aren't in the chat can leave must return true`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            GroupChatUsers.removeUsers(chatId, userId)
        }

        @Test
        fun `The users must be able to leave if the chat will be empty`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.removeUsers(chatId, adminId, userId)
        }

        @Test
        fun `The users mustn't be able to leave if the chat will have users sans admins`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertFailsWith<IllegalArgumentException> { GroupChatUsers.removeUsers(chatId, adminId) }
        }

        @Test
        fun `A non-admin must be able to leave`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            GroupChatUsers.removeUsers(chatId, userId)
        }

        @Test
        fun `An admin must be able to leave if there's another admin`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id, admin2Id))
            GroupChatUsers.removeUsers(chatId, admin1Id)
        }
    }

    @Nested
    inner class MakeAdmins {
        @Test
        fun `Setting the admin to a user who isn't in the chat must throw an exception`() {
            val (adminId, invalidAdminId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            assertFailsWith<IllegalArgumentException> { GroupChatUsers.makeAdmins(chatId, invalidAdminId) }
        }

        @Test
        fun `Only the specified users must be made admins`() {
            val (adminId, user1Id, user2Id, user3Id) = createVerifiedUsers(4).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id, user3Id))
            GroupChatUsers.makeAdmins(chatId, user1Id, user2Id)
            assertTrue(GroupChatUsers.isAdmin(user1Id, chatId))
            assertTrue(GroupChatUsers.isAdmin(user2Id, chatId))
            assertFalse(GroupChatUsers.isAdmin(user3Id, chatId))
        }

        @Test
        fun `Making admins must only notify participants`() {
            runBlocking {
                val (adminId, toBeAdminId, nonParticipantId) = createVerifiedUsers(3).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), userIdList = listOf(toBeAdminId))
                val (adminSubscriber, toBeAdminSubscriber, nonParticipantSubscriber) =
                    listOf(adminId, toBeAdminId, nonParticipantId).map { groupChatsNotifier.safelySubscribe(it) }
                GroupChatUsers.makeAdmins(chatId, toBeAdminId)
                awaitBrokering()
                listOf(adminSubscriber, toBeAdminSubscriber)
                    .forEach { it.assertValue(UpdatedGroupChat(chatId, adminIdList = listOf(adminId, toBeAdminId))) }
                nonParticipantSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class RemoveUsers {
        @Test
        fun `An exception must be thrown if the users can't leave the chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertFailsWith<IllegalArgumentException> { GroupChatUsers.removeUsers(chatId, adminId) }
        }

        @Test
        fun `Users must be removed from the chat returning false`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertFalse(GroupChatUsers.removeUsers(chatId, userId))
            assertEquals(listOf(adminId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `Participants must be notified of removed users once even if the user was removed twice`() {
            runBlocking {
                val (adminId, userId, nonParticipantId) = createVerifiedUsers(3).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), listOf(userId))
                val (adminSubscriber, userSubscriber, nonParticipantSubscriber) =
                    listOf(adminId, userId, nonParticipantId).map { groupChatsNotifier.safelySubscribe(it) }
                GroupChatUsers.removeUsers(chatId, userId, userId)
                awaitBrokering()
                adminSubscriber.assertValue(ExitedUser(userId, chatId))
                listOf(userSubscriber, nonParticipantSubscriber).forEach { it.assertNoValues() }
            }
        }

        @Test
        fun `Removing every user must delete the chat returning true`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertTrue(GroupChatUsers.removeUsers(chatId, adminId, userId))
            assertEquals(0, GroupChats.count())
        }
    }

    @Nested
    inner class IsAdmin {
        @Test
        fun `A non-admin participating in the chat mustn't be said to be an admin`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertFalse(GroupChatUsers.isAdmin(userId, chatId))
        }

        @Test
        fun `A non-participant mustn't be said to be an admin`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            assertFalse(GroupChatUsers.isAdmin(userId, chatId))
        }

        @Test
        fun `The admin of a chat must have such stated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertTrue(GroupChatUsers.isAdmin(adminId, chatId))
        }
    }
}
