package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.db.typingStatusesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.TypingStatus
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class TypingStatusesTest {
    @Nested
    inner class Update {
        @Test
        fun `Only subscribers in the chat must be notified of the status`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val (user1Subscriber, user2Subscriber, user3Subscriber) = listOf(user1Id, user2Id, user3Id)
                    .map { typingStatusesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                val isTyping = true
                TypingStatuses.update(chatId, user1Id, isTyping)
                awaitBrokering()
                listOf(user1Subscriber, user2Subscriber).forEach { subscriber ->
                    val values = subscriber.values().map { it as TypingStatus }
                    assertEquals(listOf(chatId), values.map { it.getChatId() })
                    assertEquals(listOf(user1Id), values.map { it.getUserId() })
                }
                user3Subscriber.assertNoValues()
            }
        }

        @Test
        fun `Given a typing user, when their status gets updated to say they're still typing, then their DB entry must remain, and no notifications must be sent`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatId = GroupChats.create(listOf(adminId))
                val update = { TypingStatuses.update(chatId, adminId, isTyping = true) }
                update()
                awaitBrokering()
                val subscriber = typingStatusesNotifier.subscribe(adminId).subscribeWith(TestSubscriber())
                update()
                awaitBrokering()
                assertEquals(1, TypingStatuses.count())
                subscriber.assertNoValues()
            }

        @Test
        fun `Given a typing user, when their status gets updated to say they're no longer typing, then their DB entry must get deleted, and a notification must get sent`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatId = GroupChats.create(listOf(adminId))
                TypingStatuses.update(chatId, adminId, isTyping = true)
                awaitBrokering()
                val subscriber = typingStatusesNotifier.subscribe(adminId).subscribeWith(TestSubscriber())
                TypingStatuses.update(chatId, adminId, isTyping = false)
                awaitBrokering()
                assertEquals(0, TypingStatuses.count())
                val values = subscriber.values().map { it as TypingStatus }
                assertEquals(listOf(chatId), values.map { it.getChatId() })
                assertEquals(listOf(adminId), values.map { it.getUserId() })
            }

        @Test
        fun `Given a user who isn't typing, when their status gets updated to say they're typing, then a DB must get created, and a notification must get sent`(): Unit =
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatId = GroupChats.create(listOf(adminId))
                val subscriber = typingStatusesNotifier.subscribe(adminId).subscribeWith(TestSubscriber())
                TypingStatuses.update(chatId, adminId, isTyping = true)
                awaitBrokering()
                assertEquals(1, TypingStatuses.count())
                val values = subscriber.values().map { it as TypingStatus }
                assertEquals(listOf(chatId), values.map { it.getChatId() })
                assertEquals(listOf(adminId), values.map { it.getUserId() })
            }

        @Test
        fun `Given a user who isn't typing, when their status gets updated to say they're not typing, then a DB entry musn't get created, and a notification mustn't get sent`() {
            runBlocking {
                val adminId = createVerifiedUsers(1).first().userId
                val chatId = GroupChats.create(listOf(adminId))
                val subscriber = typingStatusesNotifier.subscribe(adminId).subscribeWith(TestSubscriber())
                TypingStatuses.update(chatId, adminId, isTyping = false)
                awaitBrokering()
                assertEquals(0, TypingStatuses.count())
                subscriber.assertNoValues()
            }
        }
    }
}
