package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.UserId
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.chatsNotifier
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedPrivateChat
import com.neelkamath.omniChatBackend.linkedHashSetOf
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class PrivateChatDeletionsTest {
    @Nested
    inner class Create {
        @Test
        fun `Deleting a chat must only notify the user who deleted it`(): Unit = runBlocking {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            awaitBrokering()
            val (user1Subscriber, user2Subscriber) = setOf(user1Id, user2Id)
                .map { chatsNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
            PrivateChatDeletions.create(chatId, user1Id)
            awaitBrokering()
            val actual = user1Subscriber.values().map { (it as DeletedPrivateChat).getChatId() }
            assertEquals(listOf(chatId), actual)
            user2Subscriber.assertNoValues()
        }

        @Test
        fun `Deleting a chat the user was never in must fail`() {
            val userId = createVerifiedUsers(1).first().userId
            assertFailsWith<IllegalArgumentException> { PrivateChatDeletions.create(chatId = 1, userId = userId) }
        }

        @Test
        fun `Deleting a chat the user was in but just deleted mustn't fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            repeat(2) { PrivateChatDeletions.create(chatId, user1Id) }
        }

        @Test
        fun `Deleting the chat must unstar messages for the user even if the chat still exists`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            Stargazers.create(user1Id, messageId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(Stargazers.readMessageIdList(user1Id).isEmpty())
        }
    }

    @Nested
    inner class DeleteUnusedChatData {
        @Test
        fun `Messages deleted by one user mustn't be deleted for the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFalse(Messages.readPrivateChat(user2Id, chatId).isEmpty())
        }

        @Test
        fun `The private chat's record must be deleted if there's no activity after the both users delete it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChatDeletions.create(chatId, user2Id)
            listOf(PrivateChats, PrivateChatDeletions).forEach { assertEquals(0, it.count()) }
        }

        @Test
        fun `The private chat's record mustn't be deleted if there's activity between both users deleting it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            assertEquals(1, PrivateChats.count())
        }
    }

    @Nested
    inner class DeletePreviousDeletionRecords {
        @Test
        fun `A user must have at most one record of deleting a chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            repeat(3) { PrivateChatDeletions.create(chatId, user1Id) }
            assertEquals(1, PrivateChatDeletions.count())
        }
    }

    @Nested
    inner class ReadLastChatDeletion {
        @Test
        fun `The last deletion must be 'null' if neither user has deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            assertEquals(linkedHashSetOf(messageId), Messages.readIdList(chatId))
        }

        @Test
        fun `The last deletion must be 'null' if only one user has deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(linkedHashSetOf(messageId), Messages.readIdList(chatId))
        }

        @Test
        fun `Given one user has deleted the chat, when the other user deletes the chat, then the last deletion must be the first deletion`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message2Id = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            assertEquals(linkedHashSetOf(message2Id), Messages.readIdList(chatId))
        }
    }

    @Nested
    inner class ReadLastDeletion {
        @Test
        fun `Only messages deleted by both users must be deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message1Id = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            val message2Id = Messages.message(user1Id, chatId)
            assertEquals(linkedHashSetOf(message1Id, message2Id), Messages.readPrivateChat(user1Id, chatId))
        }
    }

    @Nested
    inner class IsDeleted {
        @Test
        fun `The chat mustn't be deleted if the user never deleted it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `The chat must be deleted if the user deleted it, and the other user didn't send a message after that`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `The chat mustn't be deleted if the other user sent a message after the user deleted it`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.message(user2Id, chatId)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `The chat mustn't be deleted if the user sent a message to the other user after deleting their chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.message(user1Id, chatId)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `A private chat deleted by one user mustn't be deleted for the other`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user2Id)
            assertFalse(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }
    }
}
