package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.deleteUser
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.testingObjectMapper
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class MessageDateTimeStatusConnectionTest {
    private data class ReadMessageResponse(val statuses: Edges) {
        data class Edges(val edges: List<Edge>) {
            data class Edge(val cursor: Cursor)
        }
    }

    private data class CreatedMessageStatuses(val adminId: Int, val messageId: Int)

    @Nested
    inner class GetEdges {
        private fun readMessage(
            userId: Int,
            messageId: Int,
            pagination: ForwardPagination? = null,
        ): ReadMessageResponse {
            val data = executeGraphQlViaEngine(
                """
                query ReadMessage(${"$"}messageId: Int!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    readMessage(messageId: ${"$"}messageId) {
                        ... on Message {
                            statuses(first: ${"$"}first, after: ${"$"}after) {
                                edges {
                                    cursor
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("messageId" to messageId, "first" to pagination?.first, "after" to pagination?.after?.toString()),
                userId,
            ).data!!["readMessage"] as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        private fun createMessageStatuses(): CreatedMessageStatuses {
            val adminId = createVerifiedUsers(1).first().userId
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), userIdList)
            val messageId = Messages.message(adminId, chatId)
            userIdList.forEach { MessageStatuses.create(it, messageId, MessageStatus.DELIVERED) }
            return CreatedMessageStatuses(adminId, messageId)
        }

        @Test
        fun `Given items, when requesting items with neither a limit nor a cursor, then every item must be retrieved`() {
            val (adminId, messageId) = createMessageStatuses()
            val actual = readMessage(adminId, messageId).statuses.edges.map { it.cursor }
            assertEquals(MessageStatuses.readIdList(messageId).toList(), actual)
        }

        @Test
        fun `Given items, when requesting items with a limit and cursor, then the number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, messageId) = createMessageStatuses()
            val statusIdList = MessageStatuses.readIdList(messageId)
            val index = 4
            val pagination = ForwardPagination(first = 3, after = statusIdList.elementAt(index))
            val actual = readMessage(adminId, messageId, pagination).statuses.edges.map { it.cursor }
            assertEquals(statusIdList.slice(index + 1..index + pagination.first!!).toList(), actual)
        }

        @Test
        fun `Given items, when requesting items with a limit but no cursor, then the number of items specified by the limit from the first item must be retrieved`() {
            val (adminId, messageId) = createMessageStatuses()
            val pagination = ForwardPagination(first = 3)
            val actual = readMessage(adminId, messageId, pagination).statuses.edges.map { it.cursor }
            assertEquals(MessageStatuses.readIdList(messageId).take(pagination.first!!), actual)
        }

        @Test
        fun `Given items, when requesting items with a cursor but no limit, then every item after the cursor must be retrieved`() {
            val (adminId, messageId) = createMessageStatuses()
            val statusIdList = MessageStatuses.readIdList(messageId)
            val index = 4
            val pagination = ForwardPagination(after = statusIdList.elementAt(index))
            val actual = readMessage(adminId, messageId, pagination).statuses.edges.map { it.cursor }
            assertEquals(statusIdList.drop(index + 1), actual)
        }

        @Test
        fun `Given items, when requesting items with the last item's cursor but no limit, then zero items must be retrieved`() {
            val (adminId, messageId) = createMessageStatuses()
            val statusIdList = MessageStatuses.readIdList(messageId)
            val pagination = ForwardPagination(after = statusIdList.last())
            assertEquals(0, readMessage(adminId, messageId, pagination).statuses.edges.size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`(): Unit =
            runBlocking {
                val (adminId, messageId) = createMessageStatuses()
                val statusIdList = MessageStatuses.readIdList(messageId)
                val userId = MessageStatuses.readUserId(statusIdList.elementAt(3))
                deleteUser(userId)
                val pagination = ForwardPagination(first = 3, after = statusIdList.elementAt(1))
                val actual = readMessage(adminId, messageId, pagination).statuses.edges.map { it.cursor }
                assertEquals(setOf(2, 4, 5).map(statusIdList::elementAt), actual)
            }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting items using the deleted item's cursor, then items 5-10 must be retrieved`(): Unit =
            runBlocking {
                val (adminId, messageId) = createMessageStatuses()
                val statusIdList = MessageStatuses.readIdList(messageId)
                val statusId = statusIdList.elementAt(3)
                val userId = MessageStatuses.readUserId(statusId)
                deleteUser(userId)
                val response = readMessage(adminId, messageId, ForwardPagination(after = statusId))
                assertEquals(statusIdList.drop(4), response.statuses.edges.map { it.cursor })
            }
    }
}
