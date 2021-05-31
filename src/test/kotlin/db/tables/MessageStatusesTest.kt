package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedMessage
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import io.reactivex.rxjava3.subscribers.TestSubscriber
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
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            val createStatus = { MessageStatuses.create(userId, messageId, MessageStatus.DELIVERED) }
            createStatus()
            assertFailsWith<IllegalArgumentException> { createStatus() }
        }

        @Test
        fun `Recording a read status mustn't create a delivered status if one was already recorded`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
            MessageStatuses.create(user2Id, messageId, MessageStatus.READ)
            assertEquals(2, MessageStatuses.count())
        }

        @Test
        fun `Recording a read status must automatically record a delivered status if there wasn't one`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            MessageStatuses.create(user2Id, messageId, MessageStatus.READ)
            assertEquals(2, MessageStatuses.count())
        }

        @Test
        fun `Creating a status for the user on their own message must throw an exception`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            assertFailsWith<IllegalArgumentException> { MessageStatuses.create(user1Id, messageId, MessageStatus.READ) }
        }

        @Test
        fun `The user mustn't be able to create a status on a message sent before they deleted the chat`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFailsWith<IllegalArgumentException> { MessageStatuses.create(user1Id, messageId, MessageStatus.READ) }
        }
    }

    @Nested
    inner class InsertAndNotify {
        @Test
        fun `Only (authenticated) subscribers in the (non-public) chat must be notified of updated statuses`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
                val chatId = PrivateChats.create(user1Id, user2Id)
                PrivateChats.create(user2Id, user3Id)
                val messageId = Messages.message(user1Id, chatId)
                awaitBrokering()
                val (user1Subscriber, user2Subscriber, user3Subscriber) = listOf(user1Id, user2Id, user3Id)
                    .map { messagesNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                MessageStatuses.create(user2Id, messageId, MessageStatus.DELIVERED)
                awaitBrokering()
                listOf(user1Subscriber, user2Subscriber).forEach { subscriber ->
                    val actual = subscriber.values().map { (it as UpdatedMessage).getMessageId() }
                    assertEquals(listOf(messageId), actual)
                }
                user3Subscriber.assertNoValues()
            }
        }

        @Test
        fun `Unauthenticated subscribers must be notified of updated statuses`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            awaitBrokering()
            val subscriber = chatMessagesNotifier.subscribe(ChatId(chatId)).subscribeWith(TestSubscriber())
            MessageStatuses.create(userId, messageId, MessageStatus.DELIVERED)
            awaitBrokering()
            val actual = subscriber.values().map { (it as UpdatedMessage).getMessageId() }
            assertEquals(listOf(messageId), actual)
        }
    }

    @Nested
    inner class DeleteUserStatuses {
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
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
            val chat1Id = createUsedChat(user1Id, user2Id)
            val chat2Id = createUsedChat(user1Id, user3Id)
            MessageStatuses.deleteUserChatStatuses(chat1Id, user1Id)
            val chat1Statuses = Messages.readIdList(chat1Id).flatMap(MessageStatuses::readIdList)
            assertTrue(chat1Statuses.isEmpty())
            val chat2Statuses = Messages.readIdList(chat2Id).flatMap(MessageStatuses::readIdList)
            assertFalse(chat2Statuses.isEmpty())
        }
    }
}
