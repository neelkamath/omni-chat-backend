package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.deleteUser
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.db.tables.message
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class PageInfoTest {
    private data class SearchUsersResponse(val pageInfo: PageInfo) {
        data class PageInfo(val hasNextPage: Boolean, val hasPreviousPage: Boolean)
    }

    private fun executeSearchUsers(pagination: ForwardPagination? = null): SearchUsersResponse.PageInfo {
        val data = executeGraphQlViaEngine(
            """
            query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
                searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
                    pageInfo {
                        hasNextPage
                        hasPreviousPage
                    }
                }
            }
            """,
            mapOf("query" to "", "first" to pagination?.first, "after" to pagination?.after?.toString()),
        ).data!!["searchUsers"] as Map<*, *>
        return testingObjectMapper.convertValue<SearchUsersResponse>(data).pageInfo
    }

    private data class ReadChatResult(val messages: Messages) {
        data class Messages(val pageInfo: PageInfo) {
            data class PageInfo(val hasNextPage: Boolean, val hasPreviousPage: Boolean)
        }
    }

    private fun executeReadChat(
        userId: Int,
        chatId: Int,
        pagination: BackwardPagination? = null,
    ): ReadChatResult.Messages.PageInfo {
        val data = executeGraphQlViaEngine(
            """
            query ReadChat(${"$"}id: Int!, ${"$"}last: Int, ${"$"}before: Cursor) {
                readChat(id: ${"$"}id) {
                    ... on Chat {
                        messages(last: ${"$"}last, before: ${"$"}before) {
                            pageInfo {
                                hasNextPage
                                hasPreviousPage
                            }
                        }
                    }
                }
            }
            """,
            mapOf("id" to chatId, "last" to pagination?.last, "before" to pagination?.before?.toString()),
            userId,
        ).data!!["readChat"] as Map<*, *>
        return testingObjectMapper.convertValue<ReadChatResult>(data).messages.pageInfo
    }

    @Nested
    inner class GetHasNextPage {
        @Test
        fun `'hasNextPage' must be 'false' when using the last item's cursor for forward pagination`() {
            val cursor = createVerifiedUsers(10).last().userId
            executeSearchUsers(ForwardPagination(after = cursor)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Retrieving the first of many items must cause 'hasNextPage' to be 'true' for forward pagination`() {
            createVerifiedUsers(10)
            assertTrue(executeSearchUsers(ForwardPagination(first = 3)).hasNextPage)
        }

        @Test
        fun `Retrieving the last of many items must cause 'hasNextPage' to be 'false' for forward pagination`() {
            val cursor = createVerifiedUsers(10).elementAt(4).userId
            executeSearchUsers(ForwardPagination(after = cursor)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Given zero items, when requesting every item, then 'hasNextPage' must be 'false' for forward pagination`(): Unit =
            assertFalse(executeSearchUsers().hasNextPage)

        @Test
        fun `Given one item, when requesting every item, then 'hasNextPage' must be 'false' for forward pagination`() {
            createVerifiedUsers(1)
            assertFalse(executeSearchUsers().hasNextPage)
        }

        @Test
        fun `Given items, when requesting zero items sans cursor, then 'hasNextPage' must be 'true' for forward pagination`() {
            createVerifiedUsers(10)
            executeSearchUsers(ForwardPagination(first = 0)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Given items, when requesting zero items after the end cursor, then 'hasNextPage' must be 'false' for forward pagination`() {
            val cursor = createVerifiedUsers(10).last().userId
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Given items, when requesting items after the start cursor, then 'hasNextPage' must be 'false' for forward pagination`() {
            val cursor = createVerifiedUsers(10).first().userId
            executeSearchUsers(ForwardPagination(after = cursor)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Given items 1-10, when requesting zero items after item 5, then 'hasNextPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).elementAt(4).userId
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Given cursors 5-10, when requesting zero items after the non-existing cursor 3, then 'hasNextPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).mapIndexed { index, (userId) ->
                if (index < 5) deleteUser(userId)
                userId
            }[2]
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Given cursors 1-5, when requesting items after the non-existing cursor 7, then 'hasNextPage' must be 'false' for forward pagination`() {
            val cursor = createVerifiedUsers(10).mapIndexed { index, (userId) ->
                if (index > 4) deleteUser(userId)
                userId
            }[6]
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Given items, when requesting items with the first item's cursor but no limit, then 'hasNextPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[0]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Retrieving the first of many items must cause 'hasNextPage' to be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[4]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Retrieving the last of many items must cause 'hasNextPage' to be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            repeat(10) { Messages.message(adminId, chatId) }
            executeReadChat(adminId, chatId, BackwardPagination(last = 3)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Given zero items, when requesting every item, then 'hasNextPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertFalse(executeReadChat(adminId, chatId).hasNextPage)
        }

        @Test
        fun `Given one item, when requesting every item, then 'hasNextPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            Messages.message(adminId, chatId)
            assertFalse(executeReadChat(adminId, chatId).hasNextPage)
        }

        @Test
        fun `Given items, when requesting zero items sans cursor, then 'hasNextPage' must be 'false'`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            executeReadChat(adminId, chatId, BackwardPagination(last = 0)).hasNextPage.let(::assertFalse)
        }

        @Test
        fun `Given items, when requesting zero items before the start cursor, then 'hasNextPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val startCursor = (1..10).map { Messages.message(adminId, chatId) }[0]
            val pagination = BackwardPagination(last = 0, before = startCursor)
            assertTrue(executeReadChat(adminId, chatId, pagination).hasNextPage)
        }

        @Test
        fun `Given items, when requesting items before the end cursor, then 'hasNextPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val endCursor = (1..10).map { Messages.message(adminId, chatId) }.last()
            executeReadChat(adminId, chatId, BackwardPagination(before = endCursor)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Given items 1-10, when requesting zero items before item 5, then 'hasNextPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[4]
            val pagination = BackwardPagination(last = 0, before = cursor)
            assertTrue(executeReadChat(adminId, chatId, pagination).hasNextPage)
        }

        @Test
        fun `Given items 1-10 where items 1-5 have been deleted, when requesting items before the deleted item 3, then 'hasNextPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { index ->
                Messages.message(adminId, chatId).also { if (index < 5) Messages.delete(it) }
            }[2]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasNextPage.let(::assertTrue)
        }

        @Test
        fun `Given items 1-10 where items 6-10 have been deleted, when requesting zero items before the deleted item 7, then 'hasNextPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { index ->
                Messages.message(adminId, chatId).also { if (index > 4) Messages.delete(it) }
            }[6]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasNextPage.let(::assertFalse)
        }
    }

    @Nested
    inner class GetPreviousPage {
        @Test
        fun `Given items, when requesting items with the last item's cursor but no limit, 'hasPreviousPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).last().userId
            executeSearchUsers(ForwardPagination(after = cursor)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Retrieving the first of many items must cause 'hasPreviousPage' to be 'false' for forward pagination`() {
            createVerifiedUsers(10)
            executeSearchUsers(ForwardPagination(first = 3)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Retrieving the last of many items must cause 'hasPreviousPage' to be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).elementAt(6).userId
            executeSearchUsers(ForwardPagination(after = cursor)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given zero items, when requesting every item, then 'hasPreviousPage' must be 'false' for forward pagination`(): Unit =
            assertFalse(executeSearchUsers().hasPreviousPage)

        @Test
        fun `Given one item, when requesting every item, then 'hasPreviousPage' must be 'false' for forward pagination`() {
            createVerifiedUsers(1)
            assertFalse(executeSearchUsers().hasPreviousPage)
        }

        @Test
        fun `Given items, when requesting zero items sans cursor, then 'hasPreviousPage' must be 'false' for forward pagination`() {
            createVerifiedUsers(10)
            executeSearchUsers(ForwardPagination(first = 0)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Given items, when requesting zero items after the end cursor, then 'hasPreviousPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).last().userId
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given items, when requesting items after the start cursor, then 'hasPreviousPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).first().userId
            executeSearchUsers(ForwardPagination(after = cursor)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given items 1-10, when requesting zero items after item 5, then 'hasPreviousPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).elementAt(4).userId
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given items 1-10 where items 1-5 have been deleted, when requesting zero items after the deleted item 3, then 'hasPreviousPage' must be 'false' for forward pagination`() {
            val cursor = createVerifiedUsers(10).mapIndexed { index, (userId) ->
                if (index < 5) deleteUser(userId)
                userId
            }[2]
            executeSearchUsers(ForwardPagination(first = 0, after = cursor)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Given items 1-10 where items 6-10 have been deleted, when requesting items after the deleted item 7, then 'hasPreviousPage' must be 'true' for forward pagination`() {
            val cursor = createVerifiedUsers(10).mapIndexed { index, (userId) ->
                if (index > 4) deleteUser(userId)
                userId
            }[6]
            executeSearchUsers(ForwardPagination(after = cursor)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given items, when requesting items with the first item's cursor but no limit, then 'hasPreviousPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[0]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Retrieving the first of many items must cause 'hasPreviousPage' to be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[3]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Retrieving the last of many items must cause 'hasPreviousPage' to be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            repeat(10) { Messages.message(adminId, chatId) }
            executeReadChat(adminId, chatId, BackwardPagination(last = 3)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given zero items, when requesting every item, then 'hasPreviousPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertFalse(executeReadChat(adminId, chatId).hasPreviousPage)
        }

        @Test
        fun `Given one item, when requesting every item, then 'hasPreviousPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            Messages.message(adminId, chatId)
            assertFalse(executeReadChat(adminId, chatId).hasPreviousPage)
        }

        @Test
        fun `Given items, when requesting zero items sans cursor, then 'hasPreviousPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            repeat(10) { Messages.message(adminId, chatId) }
            executeReadChat(adminId, chatId, BackwardPagination(last = 0)).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given items, when requesting zero items before the start cursor, then 'hasPreviousPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[0]
            val pagination = BackwardPagination(last = 0, before = cursor)
            assertFalse(executeReadChat(adminId, chatId, pagination).hasPreviousPage)
        }

        @Test
        fun `Given items, when requesting items before the end cursor, then 'hasPreviousPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }.last()
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Given items 1-10, when requesting zero items before item 5, then 'hasPreviousPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { Messages.message(adminId, chatId) }[4]
            val pagination = BackwardPagination(last = 0, before = cursor)
            executeReadChat(adminId, chatId, pagination).hasPreviousPage.let(::assertTrue)
        }

        @Test
        fun `Given items 1-10 where items 1-5 have been deleted, when requesting items before the deleted item 3, then 'hasPreviousPage' must be 'false' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { index ->
                Messages.message(adminId, chatId).also { if (index < 5) Messages.delete(it) }
            }[4]
            executeReadChat(adminId, chatId, BackwardPagination(before = cursor)).hasPreviousPage.let(::assertFalse)
        }

        @Test
        fun `Given items 1-10 where items 6-10 have been deleted, when requesting zero items before the deleted item 7, then 'hasPreviousPage' must be 'true' for backward pagination`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val cursor = (1..10).map { index ->
                Messages.message(adminId, chatId).also { if (index > 4) Messages.delete(it) }
            }[6]
            executeReadChat(adminId, chatId, BackwardPagination(last = 0, before = cursor))
                .hasPreviousPage
                .let(::assertTrue)
        }
    }
}
