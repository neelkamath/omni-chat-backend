package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.createVerifiedUsers
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertFailsWith

class GroupChatInputTest {
    @Nested
    inner class Init {
        @Test
        fun `Having zero admins should fail`() {
            assertFailsWith<IllegalArgumentException> {
                GroupChatInput(
                    GroupChatTitle("T"),
                    GroupChatDescription(""),
                    userIdList = listOf(1),
                    adminIdList = listOf(),
                    isBroadcast = false,
                    publicity = GroupChatPublicity.NOT_INVITABLE
                )
            }
        }

        @Test
        fun `An exception should be thrown if the admin ID list isn't a subset of the user ID list`() {
            assertFailsWith<IllegalArgumentException> {
                GroupChatInput(
                    GroupChatTitle("T"),
                    GroupChatDescription(""),
                    userIdList = listOf(1),
                    adminIdList = listOf(1, 2),
                    isBroadcast = false,
                    publicity = GroupChatPublicity.NOT_INVITABLE
                )
            }
        }
    }
}

class BioTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the value is too big`() {
            val value = CharArray(Bio.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { Bio(value) }
        }
    }
}

class MessageTextTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the value is too short`() {
            assertFailsWith<IllegalArgumentException> { MessageText("") }
        }

        @Test
        fun `An exception should be thrown if the value is only whitespace`() {
            assertFailsWith<IllegalArgumentException> { MessageText("  ") }
        }

        @Test
        fun `An exception should be thrown if the value is too long`() {
            val text = CharArray(MessageText.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { MessageText(text) }
        }
    }
}

class GroupChatTitleTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the title is only whitespace`() {
            assertFailsWith<IllegalArgumentException> { GroupChatTitle("  ") }
        }

        @Test
        fun `An exception should be thrown if the title is too long`() {
            val title = CharArray(GroupChatTitle.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { GroupChatTitle(title) }
        }

        @Test
        fun `An exception should be thrown if the title is too short`() {
            assertFailsWith<IllegalArgumentException> { GroupChatTitle("") }
        }
    }
}

class GroupChatDescriptionTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the description is too long`() {
            val description = CharArray(GroupChatDescription.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { GroupChatDescription(description) }
        }
    }
}

class UsernameTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown for a username which doesn't contain non-whitespace characters`() {
            assertFailsWith<IllegalArgumentException> { Username("  ") }
        }

        @Test
        fun `An exception should be thrown for a username which isn't lowercase`() {
            assertFailsWith<IllegalArgumentException> { Username("Username") }
        }

        @Test
        fun `An exception should be thrown for a username greater than 255 characters`() {
            val value = CharArray(256) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { Username(value) }
        }
    }
}

class PasswordTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the password doesn't contain non-whitespace characters`() {
            assertFailsWith<IllegalArgumentException> { Password("  ") }
        }
    }
}

class UpdatedGroupChatTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception should be thrown if the new and removed users intersect`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            assertFailsWith<IllegalArgumentException> {
                UpdatedGroupChat(chatId = 1, newUsers = listOf(user1), removedUsers = listOf(user1, user2))
            }
        }
    }
}

class ModelsTest {
    @Nested
    inner class AssertOptions {
        @Test
        fun `An exception should be thrown if there are fewer than two options`() {
            val options = listOf(MessageText("option 1"))
            assertFailsWith<IllegalArgumentException> { PollInput(MessageText("Title"), options) }
        }

        @Test
        fun `An exception should be thrown if the options aren't unique`() {
            val option = MessageText("option")
            assertFailsWith<IllegalArgumentException> { PollInput(MessageText("Title"), listOf(option, option)) }
        }
    }
}