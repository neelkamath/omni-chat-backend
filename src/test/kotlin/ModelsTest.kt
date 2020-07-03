package com.neelkamath.omniChat

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class UsernameTest : FunSpec({
    context("init") {
        test("An exception should be thrown for a username which doesn't contain non-whitespace characters") {
            shouldThrowExactly<IllegalArgumentException> { Username("  ") }
        }

        test("An exception should be thrown for a username which isn't lowercase") {
            shouldThrowExactly<IllegalArgumentException> { Username("Username") }
        }

        test("An exception should be thrown for a username greater than 255 characters") {
            val value = CharArray(256) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { Username(value) }
        }
    }
})

class PasswordTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the password doesn't contain non-whitespace characters") {
            shouldThrowExactly<IllegalArgumentException> { Password("  ") }
        }
    }
})

class VerifyGroupChatUsersTest : FunSpec({
    test("An exception should be thrown if the list of users to add isn't distinct from the list of users to remove") {
        val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
        shouldThrowExactly<IllegalArgumentException> {
            GroupChatUpdate(
                chatId = 1,
                newUserIdList = listOf(user1Id, user2Id),
                removedUserIdList = listOf(user2Id, user3Id)
            )
        }
    }

    test("An exception shouldn't be thrown if the list of users to add is distinct from the list of users to remove") {
        shouldNotThrowAny {
            GroupChatUpdate(chatId = 1, newUserIdList = listOf("user 1 ID"), removedUserIdList = listOf("user 2 ID"))
        }
    }
})