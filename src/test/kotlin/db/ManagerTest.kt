package com.neelkamath.omniChatBackend.db

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedAccount
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.DeletedPrivateChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChat
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ExtendWith(DbExtension::class)
class ManagerTest {
    @Nested
    inner class DeleteUser {
        @Test
        fun `The other users must get notified of the deleted private chats`(): Unit = runBlocking {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
            val (chat1Id, chat2Id) = setOf(user2Id, user3Id).map { PrivateChats.create(user1Id, it) }
            PrivateChats.create(user2Id, user3Id)
            awaitBrokering()
            val (user1Subscriber, user2Subscriber, user3Subscriber) = setOf(user1Id, user2Id, user3Id)
                .map { chatsNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
            deleteUser(user1Id)
            awaitBrokering()
            user1Subscriber.assertNoValues()
            mapOf(user2Subscriber to chat1Id, user3Subscriber to chat2Id).forEach { (subscriber, chatId) ->
                val actual = subscriber.values().filterIsInstance<DeletedPrivateChat>().map { it.getChatId() }
                assertEquals(listOf(chatId), actual)
            }
        }

        @Test
        fun `An exception must be thrown when the admin of a nonempty group chat deletes their data`(): Unit =
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
                GroupChats.create(setOf(adminId), setOf(userId))
                assertFailsWith<IllegalArgumentException> { deleteUser(adminId) }
            }

        @Test
        fun `The deleted user must be unsubscribed via the new group chats broker`() {
            runBlocking {
                val userId = createVerifiedUsers(1).first().userId
                val subscriber = chatsNotifier.subscribe(UserId(userId)).flowable.subscribeWith(TestSubscriber())
                deleteUser(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `A private chat must be deleted for the other user if the user deleted it before deleting their data`(): Unit =
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
                val chatId = PrivateChats.create(user1Id, user2Id)
                PrivateChatDeletions.create(chatId, user1Id)
                deleteUser(user1Id)
                assertEquals(0, PrivateChats.count())
            }

        @Test
        fun `The deleted user must be unsubscribed from contact updates`() {
            runBlocking {
                val userId = createVerifiedUsers(1).first().userId
                val subscriber = accountsNotifier.subscribe(UserId(userId)).flowable.subscribeWith(TestSubscriber())
                deleteUser(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `Only the deleted subscriber must be unsubscribed from updated chats`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
                GroupChats.create(setOf(adminId), setOf(userId))
                awaitBrokering()
                val (adminSubscriber, userSubscriber) = setOf(adminId, userId)
                    .map { chatsNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
                deleteUser(userId)
                awaitBrokering()
                val expected = listOf(listOf(userId))
                val actual = adminSubscriber.values().map { value ->
                    (value as UpdatedGroupChat).getRemovedUsers()!!.map { it.getUserId() }
                }
                assertEquals(expected, actual)
                userSubscriber.assertComplete()
            }
        }

        @Test
        fun `The user must be unsubscribed from message updates`() {
            runBlocking {
                val userId = createVerifiedUsers(1).first().userId
                val subscriber = messagesNotifier.subscribe(UserId(userId)).flowable.subscribeWith(TestSubscriber())
                deleteUser(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `Contacts and chat sharers must be notified of the deleted user`(): Unit = runBlocking {
            val (userId, contactId, chatSharerId) = createVerifiedUsers(3).map { it.userId }
            Contacts.create(contactId, userId)
            PrivateChats.create(userId, chatSharerId)
            awaitBrokering()
            val (contactSubscriber, chatSharerSubscriber) = setOf(contactId, chatSharerId)
                .map { accountsNotifier.subscribe(UserId(it)).flowable.subscribeWith(TestSubscriber()) }
            deleteUser(userId)
            awaitBrokering()
            setOf(contactSubscriber, chatSharerSubscriber).forEach { subscriber ->
                val actual = subscriber.values().map { if (it is DeletedAccount) it.getUserId() else null }
                assertContains(actual, userId)
            }
        }
    }

    @Nested
    inner class SearchUsers {
        @Test
        fun `Users must be searched case-insensitively`() {
            val (blocker, blocked1, blocked2) = createVerifiedUsers(3)
            setOf(blocked1, blocked2).forEach { BlockedUsers.create(blocker.userId, it.userId) }
            val actual =
                BlockedUsers.search(blocker.userId, query = blocked1.username.value.uppercase(Locale.getDefault()))
            assertEquals(linkedHashSetOf(blocked1.userId), actual)
        }
    }
}
