package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.typingStatusesNotifier
import com.neelkamath.omniChat.graphql.routing.TypingStatus
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@ExtendWith(DbExtension::class)
class TypingStatusesTest {
    @Nested
    inner class Set {
        @Test
        fun `Only subscribers in the chat must be notified of the status`() {
            runBlocking {
                val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
                val chatId = PrivateChats.create(user1Id, user2Id)
                val (user1Subscriber, user2Subscriber, user3Subscriber) = listOf(user1Id, user2Id, user3Id)
                    .map { typingStatusesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                val isTyping = true
                TypingStatuses.set(chatId, user1Id, isTyping)
                awaitBrokering()
                listOf(user1Subscriber, user3Subscriber).forEach { it.assertNoValues() }
                user2Subscriber.assertValue(TypingStatus(chatId, user1Id, isTyping))
            }
        }

        private fun assertSet(repetitions: Int) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            repeat(repetitions) {
                TypingStatuses.set(chatId, adminId, isTyping = true)
                assertEquals(1, TypingStatuses.count())
            }
        }

        @Test
        fun `A new record must be created when setting a status for the first time`() {
            assertSet(repetitions = 1)
        }

        @Test
        fun `The existing record must be updated when setting a status the second time`() {
            assertSet(repetitions = 2)
        }
    }

    @Nested
    inner class Read {
        @Test
        fun `The status must be read`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val isTyping = true
            TypingStatuses.set(chatId, adminId, isTyping)
            assertEquals(isTyping, TypingStatuses.read(chatId, adminId))
        }

        @Test
        fun `false must be returned when reading a nonexistent status`() {
            assertFalse(TypingStatuses.read(chatId = 1, userId = 1))
        }
    }

    @Nested
    inner class ReadChats {
        @Test
        fun `Only users who are typing must be returned excluding the user themselves`() {
            val (adminId, participant1Id, participant2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(participant1Id, participant2Id))
            TypingStatuses.set(chatId, adminId, isTyping = true)
            TypingStatuses.set(chatId, participant1Id, isTyping = true)
            val expected = setOf(TypingStatus(chatId, participant1Id, isTyping = true))
            val actual = TypingStatuses.readChats(setOf(chatId), adminId)
            assertEquals(expected, actual)
        }
    }
}
