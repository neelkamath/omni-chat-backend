package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test

@ExtendWith(DbExtension::class)
class StargazersTest {
    @Nested
    inner class Create {
        @Test
        fun `Starring must only notify the stargazer`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                    .map { messagesNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                Stargazers.create(user1Id, messageId)
                awaitBrokering()
                user1Subscriber.assertValue(Messages.readMessage(user1Id, messageId).toUpdatedTextMessage())
                user2Subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class DeleteUserStar {
        @Test
        fun `Deleting a star must only notify the deleter`() {
            runBlocking {
                val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val messageId = Messages.message(user1Id, chatId)
                Stargazers.create(user1Id, messageId)
                val (user1Subscriber, user2Subscriber) = listOf(user1Id, user2Id)
                    .map { messagesNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                Stargazers.deleteUserStar(user1Id, messageId)
                awaitBrokering()
                user1Subscriber.assertValue(Messages.readMessage(user1Id, messageId).toUpdatedTextMessage())
                user2Subscriber.assertNoValues()
            }
        }

        @Test
        fun `Deleting a nonexistent star mustn't cause anything to happen`() {
            runBlocking {
                val adminId = createVerifiedUsers(1)[0].info.id
                val chatId = GroupChats.create(listOf(adminId))
                val messageId = Messages.message(adminId, chatId)
                val subscriber =
                    messagesNotifier.safelySubscribe(adminId).subscribeWith(TestSubscriber())
                Stargazers.deleteUserStar(adminId, messageId)
                awaitBrokering()
                subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class DeleteStar {
        @Test
        fun `Deleting a message's stars must only notify its stargazers`() {
            runBlocking {
                val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId), listOf(user1Id, user2Id))
                val messageId = Messages.message(adminId, chatId)
                listOf(adminId, user1Id).forEach { Stargazers.create(it, messageId) }
                val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                    .map { messagesNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                Stargazers.deleteStar(messageId)
                awaitBrokering()
                mapOf(adminId to adminSubscriber, user1Id to user1Subscriber).forEach { (userId, subscriber) ->
                    subscriber.assertValue(Messages.readMessage(userId, messageId).toUpdatedTextMessage())
                }
                user2Subscriber.assertNoValues()
            }
        }
    }
}
