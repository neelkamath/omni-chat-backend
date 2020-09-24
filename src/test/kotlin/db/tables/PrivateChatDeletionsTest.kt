package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.count
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class PrivateChatDeletionsTest {
    @Nested
    inner class IsDeleted {
        @Test
        fun `The chat shouldn't be deleted if the user never deleted it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `The chat should be deleted if the user deleted it, and the other user didn't send a message after that`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `The chat shouldn't be deleted if the other user sent a message after the user deleted it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(user2Id, chatId)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `The chat shouldn't be deleted if the user sent a message to the other user after deleting their chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(user1Id, chatId)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `A private chat deleted by one user shouldn't be deleted for the other`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user2Id)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `Deleting a chat the user was never in should fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertFailsWith<IllegalArgumentException> { PrivateChatDeletions.create(chatId = 1, userId = userId) }
        }

        @Test
        fun `Deleting a chat the user was in but just deleted shouldn't fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            repeat(2) { PrivateChatDeletions.create(chatId, user1Id) }
        }
    }

    @Nested
    inner class ReadLastDeletion {
        @Test
        fun `Only messages deleted by both users should be deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message1Id = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            val message2Id = Messages.message(user1Id, chatId)
            assertEquals(listOf(message1Id, message2Id), Messages.readPrivateChat(user1Id, chatId).map { it.cursor })
        }
    }

    @Nested
    inner class DeletePreviousDeletionRecords {
        @Test
        fun `A user must have at most one record of deleting a chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            repeat(3) { PrivateChatDeletions.create(chatId, user1Id) }
            assertEquals(1, PrivateChatDeletions.count())
        }
    }

    @Nested
    inner class DeleteUnusedChatData {
        @Test
        fun `Messages deleted by one user shouldn't be deleted for the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFalse(Messages.readPrivateChat(user2Id, chatId).isEmpty())
        }

        @Test
        fun `The private chat's record should be deleted if there's no activity after the both users delete it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChatDeletions.create(chatId, user2Id)
            listOf(PrivateChats, PrivateChatDeletions).forEach { assertEquals(0, it.count()) }
        }

        @Test
        fun `The private chat's record shouldn't be deleted if there's activity between both users deleting it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            assertEquals(1, PrivateChats.count())
        }
    }
}
