package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.Users
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertFailsWith

fun AccountInput.toAccount(): Account =
    Account(Users.read(username).id, username, emailAddress, firstName, lastName, bio)

fun ActionMessageInput.toActionableMessage(): ActionableMessage = ActionableMessage(text, actions)

class GroupChatInputTest {
    @Nested
    inner class Init {
        @Test
        fun `Having zero admins must fail`() {
            assertFailsWith<IllegalArgumentException> {
                GroupChatInput(
                    GroupChatTitle("T"),
                    GroupChatDescription(""),
                    userIdList = listOf(1),
                    adminIdList = listOf(),
                    isBroadcast = false,
                    publicity = GroupChatPublicity.NOT_INVITABLE,
                )
            }
        }

        @Test
        fun `An exception must be thrown if the admin ID list isn't a subset of the user ID list`() {
            assertFailsWith<IllegalArgumentException> {
                GroupChatInput(
                    GroupChatTitle("T"),
                    GroupChatDescription(""),
                    userIdList = listOf(1),
                    adminIdList = listOf(1, 2),
                    isBroadcast = false,
                    publicity = GroupChatPublicity.NOT_INVITABLE,
                )
            }
        }
    }
}

class BioTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the value is too big`() {
            val value = CharArray(Bio.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { Bio(value) }
        }

        @Test
        fun `An exception must be thrown if leading or trailing whitespace exists`() {
            assertFailsWith<IllegalArgumentException> { Bio(" Dev ") }
        }
    }
}

class MessageTextTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the value contains leading or trailing whitespace`() {
            assertFailsWith<IllegalArgumentException> { MessageText(" text ") }
        }

        @Test
        fun `An exception must be thrown if the value is too short`() {
            assertFailsWith<IllegalArgumentException> { MessageText("") }
        }

        @Test
        fun `An exception must be thrown if the value is only whitespace`() {
            assertFailsWith<IllegalArgumentException> { MessageText("  ") }
        }

        @Test
        fun `An exception must be thrown if the value is too long`() {
            val text = CharArray(MessageText.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { MessageText(text) }
        }
    }
}

class GroupChatTitleTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the value contains leading or trailing whitespace`() {
            assertFailsWith<IllegalArgumentException> { GroupChatTitle(" T ") }
        }

        @Test
        fun `An exception must be thrown if the title is only whitespace`() {
            assertFailsWith<IllegalArgumentException> { GroupChatTitle("  ") }
        }

        @Test
        fun `An exception must be thrown if the title is too long`() {
            val title = CharArray(GroupChatTitle.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { GroupChatTitle(title) }
        }

        @Test
        fun `An exception must be thrown if the title is too short`() {
            assertFailsWith<IllegalArgumentException> { GroupChatTitle("") }
        }
    }
}

class GroupChatDescriptionTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the value contains trailing whitespace`() {
            assertFailsWith<IllegalArgumentException> { GroupChatDescription(" description ") }
        }

        @Test
        fun `An exception must be thrown if the description is too long`() {
            val description = CharArray(GroupChatDescription.MAX_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { GroupChatDescription(description) }
        }
    }
}

class NameTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown for a name which contains whitespace characters`() {
            assertFailsWith<IllegalArgumentException> { Name("john doe") }
        }

        @Test
        fun `An exception must be thrown for a name greater than 30 characters`() {
            val value = CharArray(Users.MAX_NAME_LENGTH + 1) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { Name(value) }
        }
    }
}

class UsernameTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown for a username which contains whitespace`() {
            assertFailsWith<IllegalArgumentException> { Username("john doe") }
        }

        @Test
        fun `An exception must be thrown for a username which isn't lowercase`() {
            assertFailsWith<IllegalArgumentException> { Username("Username") }
        }

        @Test
        fun `An exception must be thrown for a username which is too short`() {
            assertFailsWith<IllegalArgumentException> { Username("") }
        }

        @Test
        fun `An exception must be thrown for a username which is too long`() {
            val value = CharArray(31) { 'a' }.joinToString("")
            assertFailsWith<IllegalArgumentException> { Username(value) }
        }

        @Test
        fun `Lowercase English letters (a-z), English numbers (0-9), periods, and underscores must be allowed`() {
            Username("a0._")
        }

        @Test
        fun `Non-English characters mustn't be allowed`() {
            assertFailsWith<IllegalArgumentException> { Username("诶") }
        }

        @Test
        fun `Emoji mustn't be allowed`() {
            assertFailsWith<IllegalArgumentException> { Username("\uD83D\uDE00") }
        }
    }
}

class PasswordTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the password doesn't contain non-whitespace characters`() {
            assertFailsWith<IllegalArgumentException> { Password("  ") }
        }
    }
}

@ExtendWith(DbExtension::class)
class UpdatedGroupChatTest {
    @Nested
    inner class Init {
        @Test
        fun `An exception must be thrown if the new and removed users intersect`() {
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
        fun `An exception must be thrown if there are fewer than two options`() {
            val options = listOf(MessageText("option 1"))
            assertFailsWith<IllegalArgumentException> { PollInput(MessageText("Title"), options) }
        }

        @Test
        fun `An exception must be thrown if the options aren't unique`() {
            val option = MessageText("option")
            assertFailsWith<IllegalArgumentException> { PollInput(MessageText("Title"), listOf(option, option)) }
        }
    }
}
