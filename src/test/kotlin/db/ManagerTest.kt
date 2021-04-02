package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.routing.DeletedAccount
import com.neelkamath.omniChat.graphql.routing.ExitedUsers
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class DbTest {
    @Nested
    inner class DeleteUser {
        @Test
        fun `An exception must be thrown when the admin of a nonempty group chat deletes their data`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertFailsWith<IllegalArgumentException> { deleteUser(adminId) }
        }

        @Test
        fun `The deleted user must be unsubscribed via the new group chats broker`() {
            runBlocking {
                val userId = createVerifiedUsers(1).first().info.id
                val subscriber = groupChatsNotifier.subscribe(userId).subscribeWith(TestSubscriber())
                deleteUser(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `A private chat must be deleted for the other user if the user deleted it before deleting their data`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            deleteUser(user1Id)
            assertEquals(0, PrivateChats.count())
        }

        @Test
        fun `The deleted user must be unsubscribed from contact updates`() {
            runBlocking {
                val userId = createVerifiedUsers(1).first().info.id
                val subscriber = accountsNotifier.subscribe(userId).subscribeWith(TestSubscriber())
                deleteUser(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `Only the deleted subscriber must be unsubscribed from updated chats`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), listOf(userId))
                awaitBrokering()
                val (adminSubscriber, userSubscriber) =
                    listOf(adminId, userId).map { groupChatsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                deleteUser(userId)
                awaitBrokering()
                val exitedUsers = ExitedUsers(chatId, listOf(userId))
                adminSubscriber.assertValue(exitedUsers)
                userSubscriber.assertComplete()
            }
        }

        @Test
        fun `The user must be unsubscribed from message updates`() {
            runBlocking {
                val userId = createVerifiedUsers(1).first().info.id
                val subscriber = messagesNotifier.subscribe(userId).subscribeWith(TestSubscriber())
                deleteUser(userId)
                subscriber.assertComplete()
            }
        }

        @Test
        fun `Contacts and chat sharers must be notified of the deleted user`(): Unit = runBlocking {
            val (userId, contactId, chatSharerId) = createVerifiedUsers(3).map { it.info.id }
            Contacts.create(contactId, userId)
            PrivateChats.create(userId, chatSharerId)
            awaitBrokering()
            val (contactSubscriber, chatSharerSubscriber) =
                listOf(contactId, chatSharerId).map { accountsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            deleteUser(userId)
            awaitBrokering()
            val account = DeletedAccount(userId)
            listOf(contactSubscriber, chatSharerSubscriber).forEach { assertTrue(it.values().contains(account)) }
        }
    }
}
