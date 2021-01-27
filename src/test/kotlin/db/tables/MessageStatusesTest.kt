package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.MessageStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class MessageStatusesTest {
    @Nested
    inner class Create {
        @Test
        fun `Saving a duplicate message status must throw an exception`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            val createStatus = { MessageStatuses.create(userId, messageId, MessageStatus.DELIVERED) }
            createStatus()
            assertFailsWith<IllegalArgumentException> { createStatus() }
        }

        @Test
        fun `Recording a read status mustn't create a delivered status if one was already recorded`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
            MessageStatuses.create(user2Id, messageId, MessageStatus.READ)
            assertEquals(2, MessageStatuses.count())
        }

        @Test
        fun `Recording a read status must automatically record a delivered status if there wasn't one`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            MessageStatuses.create(user2Id, messageId, MessageStatus.READ)
            assertEquals(2, MessageStatuses.count())
        }

        @Test
        fun `Creating a status for the user on their own message must throw an exception`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            assertFailsWith<IllegalArgumentException> { MessageStatuses.create(user1Id, messageId, MessageStatus.READ) }
        }

        @Test
        fun `The user mustn't be able to create a status on a message sent before they deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFailsWith<IllegalArgumentException> { MessageStatuses.create(user1Id, messageId, MessageStatus.READ) }
        }
    }

    @Nested
    inner class InsertAndNotify {
        @Test
        fun `Only subscribers in the chat must be notified of updated statuses`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                PrivateChats.create(user2Id, user3Id)
                val messageId = Messages.message(user1Id, chatId)
                val (user1Subscriber, user2Subscriber, user3Subscriber) =
                    listOf(user1Id, user2Id, user3Id).map { messagesNotifier.safelySubscribe(it) }
                MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
                awaitBrokering()
                mapOf(user1Subscriber to user1Id, user2Subscriber to user2Id).forEach { (subscriber, userId) ->
                    Messages.readMessage(userId, messageId).toUpdatedTextMessage().let(subscriber::assertValue)
                }
                user3Subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class Delete {
        /**
         * Creates a private chat between [user1Id] and [user2Id], has [user2Id] send a message in it, has [user1Id]
         * create a [MessageStatus.DELIVERED] on it, and returns the chat's ID.
         */
        private fun createUsedChat(user1Id: Int, user2Id: Int): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            MessageStatuses.create(user1Id, messageId, MessageStatus.DELIVERED)
            return chatId
        }

        @Test
        fun `The user's statuses must only be deleted for the chat in question`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chat1Id = createUsedChat(user1Id, user2Id)
            val chat2Id = createUsedChat(user1Id, user3Id)
            MessageStatuses.deleteUserChatStatuses(chat1Id, user1Id)
            assertTrue(Messages.readPrivateChat(user1Id, chat1Id).flatMap { it.node.dateTimes.statuses }.isEmpty())
            assertFalse(Messages.readPrivateChat(user1Id, chat2Id).flatMap { it.node.dateTimes.statuses }.isEmpty())
        }
    }
}
