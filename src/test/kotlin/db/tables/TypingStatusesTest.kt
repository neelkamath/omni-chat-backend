package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.TypingStatusesAsset
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.typingStatusesBroker
import com.neelkamath.omniChat.graphql.routing.TypingStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class TypingStatusesTest : FunSpec({
    context("set(Int, Int, Boolean)") {
        test("Only subscribers in the chat should be notified of the status") {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (user1Subscriber, user2Subscriber, user3Subscriber) = listOf(user1Id, user2Id, user3Id)
                .map { typingStatusesBroker.subscribe(TypingStatusesAsset(it)).subscribeWith(TestSubscriber()) }
            val isTyping = true
            TypingStatuses.set(chatId, user1Id, isTyping)
            listOf(user1Subscriber, user3Subscriber).forEach { it.assertNoValues() }
            user2Subscriber.assertValue(TypingStatus(chatId, user1Id, isTyping))
        }

        fun assertSet(repetitions: Int) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            repeat(repetitions) {
                TypingStatuses.set(chatId, adminId, isTyping = true)
                TypingStatuses.count() shouldBe 1
            }
        }

        test("A new record should be created when setting a status for the first time") { assertSet(repetitions = 1) }

        test("The existing record should be updated when setting a status the second time") {
            assertSet(repetitions = 2)
        }
    }

    context("read(Int, Int)") {
        test("The status should be read") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val isTyping = true
            TypingStatuses.set(chatId, adminId, isTyping)
            TypingStatuses.read(chatId, adminId) shouldBe isTyping
        }

        test(""""false" should be returned when reading a nonexistent status""") {
            TypingStatuses.read(chatId = 1, userId = 1).shouldBeFalse()
        }
    }
})