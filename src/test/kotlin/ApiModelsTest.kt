package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.Users
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec

class GroupChatInputTest : FunSpec({
    context("init") {
        test("Having zero admins should fail") {
            shouldThrowExactly<IllegalArgumentException> {
                GroupChatInput(
                    GroupChatTitle("T"),
                    GroupChatDescription(""),
                    userIdList = listOf(1),
                    adminIdList = listOf(),
                    isBroadcast = false

                )
            }
        }

        test("An exception should be thrown if the admin ID list isn't a subset of the user ID list") {
            shouldThrowExactly<IllegalArgumentException> {
                GroupChatInput(
                    GroupChatTitle("T"),
                    GroupChatDescription(""),
                    userIdList = listOf(1),
                    adminIdList = listOf(1, 2),
                    isBroadcast = false
                )
            }
        }
    }
})

class BioTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the value is too big") {
            val value = CharArray(Users.MAX_BIO_LENGTH + 1) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { Bio(value) }
        }
    }
})

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

class UpdatedGroupChatTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the new and removed users intersect") {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            shouldThrowExactly<IllegalArgumentException> {
                UpdatedGroupChat(chatId = 1, newUsers = listOf(user1), removedUsers = listOf(user1, user2))
            }
        }
    }
})