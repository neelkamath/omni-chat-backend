package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class TextMessageTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the value is too short") {
            shouldThrowExactly<IllegalArgumentException> { TextMessage("") }
        }

        test("An exception should be thrown if the value is only whitespace") {
            shouldThrowExactly<IllegalArgumentException> { TextMessage("  ") }
        }

        test("An exception should be thrown if the value is too long") {
            val text = CharArray(Messages.MAX_TEXT_LENGTH + 1) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { TextMessage(text) }
        }
    }
})

class GroupChatTitleTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the title is only whitespace") {
            shouldThrowExactly<IllegalArgumentException> { GroupChatTitle("  ") }
        }

        test("An exception should be thrown if the title is too long") {
            val title = CharArray(GroupChats.MAX_TITLE_LENGTH + 1) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { GroupChatTitle(title) }
        }

        test("An exception should be thrown if the title is too short") {
            shouldThrowExactly<IllegalArgumentException> { GroupChatTitle("") }
        }
    }
})

class GroupChatDescriptionTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the description is too long") {
            val description = CharArray(GroupChats.MAX_DESCRIPTION_LENGTH + 1) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { GroupChatDescription(description) }
        }
    }
})

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