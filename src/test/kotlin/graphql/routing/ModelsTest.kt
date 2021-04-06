package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
import com.neelkamath.omniChat.slice
import com.neelkamath.omniChat.toLinkedHashSet
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

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

@ExtendWith(DbExtension::class)
@Suppress("ClassName")
class AccountsConnectionTest {
    @Nested
    inner class Companion_build {
        /** Creates [count] users. */
        private fun createAccountEdges(count: Int = 10): LinkedHashSet<AccountEdge> = createVerifiedUsers(count)
            .zip(Users.read())
            .map { (user, cursor) -> AccountEdge(user.info, cursor) }
            .toLinkedHashSet()

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val edges = createAccountEdges()
            assertEquals(edges, AccountsConnection.build(edges).edges.toSet())
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val edges = createAccountEdges()
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, edges.elementAt(index).cursor)
            val actual = AccountsConnection.build(edges, pagination).edges.toLinkedHashSet()
            assertEquals(edges.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val edges = createAccountEdges()
            val first = 3
            val actual = AccountsConnection.build(edges, ForwardPagination(first)).edges
            assertEquals(edges.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val edges = createAccountEdges()
            val index = 5
            val pagination = ForwardPagination(after = edges.elementAt(index).cursor)
            assertEquals(edges.drop(index + 1), AccountsConnection.build(edges, pagination).edges)
        }

        @Test
        fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {
            val accountEdges = createAccountEdges()
            val pagination = ForwardPagination(after = accountEdges.last().cursor)
            val (edges, pageInfo) = AccountsConnection.build(accountEdges, pagination)
            assertEquals(0, edges.size)
            assertFalse(pageInfo.hasNextPage)
            assertTrue(pageInfo.hasPreviousPage)
        }

        @Test
        fun `When requesting items after the start cursor, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {
            val edges = createAccountEdges()
            val pagination = ForwardPagination(after = edges.first().cursor)
            val (hasNextPage, hasPreviousPage) = AccountsConnection.build(edges, pagination).pageInfo
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val edges = createAccountEdges()
            val expected = listOf(edges.elementAt(2), edges.elementAt(4), edges.elementAt(5))
            val pagination = ForwardPagination(first = 3, after = edges.elementAt(1).cursor)
            val modifiedEdges = edges.withIndex().filter { it.index != 3 }.map { it.value }.toSet()
            val actual = AccountsConnection.build(modifiedEdges, pagination).edges
            assertEquals(expected, actual)
        }

        @Test
        fun `Using a deleted user's cursor must cause pagination to work as if the user still exists`() {
            val edges = createAccountEdges()
            val index = 5
            val deletedUser = edges.elementAt(index)
            Users.delete(deletedUser.node.id)
            val first = 3
            val actual =
                AccountsConnection.build(edges, ForwardPagination(first, deletedUser.cursor)).edges.toLinkedHashSet()
            assertEquals(edges.slice(index + 1..index + first), actual)
        }

        @Test
        fun `When retrieving the first of many users, the page info must state that there are only users after`(): Unit =
            AccountsConnection.build(createAccountEdges(), ForwardPagination(first = 1)).pageInfo.run {
                assertTrue(hasNextPage)
                assertFalse(hasPreviousPage)
            }

        @Test
        fun `Retrieving the last user must cause the page info to state there are only users before`() {
            val edges = createAccountEdges()
            AccountsConnection.build(edges, ForwardPagination(after = edges.elementAt(1).cursor)).pageInfo.run {
                assertFalse(hasNextPage)
                assertTrue(hasPreviousPage)
            }
        }

        @Test
        fun `If there are zero items, the page info must indicate such`() {
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            val actual = AccountsConnection.build(accountEdges = setOf()).pageInfo
            assertEquals(expected, actual)
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val edges = createAccountEdges(1)
            val expected = PageInfo(
                hasNextPage = false,
                hasPreviousPage = false,
                startCursor = edges.first().cursor,
                endCursor = edges.last().cursor,
            )
            assertEquals(expected, AccountsConnection.build(edges).pageInfo)
        }

        @Test
        fun `When requesting zero items sans cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val edges = createAccountEdges()
            val (hasNextPage, hasPreviousPage) = AccountsConnection.build(edges, ForwardPagination(first = 0)).pageInfo
            assertTrue(hasNextPage)
            assertFalse(hasPreviousPage)
        }

        @Test
        fun `When requesting zero items after the end cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val edges = createAccountEdges()
            val pagination = ForwardPagination(first = 0, after = edges.last().cursor)
            val (hasNextPage, hasPreviousPage) = AccountsConnection.build(edges, pagination).pageInfo
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Given items 1-10, when requesting zero items after item 5, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val edges = createAccountEdges()
            val pagination = ForwardPagination(first = 0, after = edges.elementAt(4).cursor)
            val (hasNextPage, hasPreviousPage) = AccountsConnection.build(edges, pagination).pageInfo
            assertTrue(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `The first and last cursors must be the first and last users respectively`() {
            val edges = createAccountEdges()
            AccountsConnection.build(edges).pageInfo.run {
                assertEquals(edges.first().cursor, startCursor)
                assertEquals(edges.last().cursor, endCursor)
            }
        }

        @Test
        fun `Supplying unsorted rows mustn't affect pagination`() {
            val edges = createVerifiedUsers(10)
                .zip(Users.read())
                .map { (user, cursor) -> AccountEdge(user.info, cursor) }
            val first = 3
            val index = 5
            val actual =
                AccountsConnection.build(edges.shuffled().toSet(), ForwardPagination(first, edges[index].cursor)).edges
            assertEquals(edges.slice(index + 1..index + first), actual)
        }
    }
}
