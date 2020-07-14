package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

class TypingStatusesTest : FunSpec({
    context("read(Int, Int)") {
        test("The status should be read") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val isTyping = true
            TypingStatuses.set(adminId, chatId, isTyping)
            TypingStatuses.read(adminId, chatId) shouldBe isTyping
        }

        test(""""false" should be returned when reading a nonexistent status""") {
            TypingStatuses.read(userId = 1, chatId = 1).shouldBeFalse()
        }
    }

    context("set(Int, Int, Boolean)") {
        fun assertSet(repetitions: Int) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            repeat(repetitions) {
                TypingStatuses.set(adminId, chatId, isTyping = true)
                TypingStatuses.count() shouldBe 1
            }
        }

        test("A new record should be created when setting a status for the first time") { assertSet(repetitions = 1) }

        test("The existing record should be updated when setting a status the second time") {
            assertSet(repetitions = 2)
        }
    }
})