package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class StarredMessageTest {
    private data class ReadStarsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val statuses: Statuses) {
                data class Statuses(val edges: List<Edge>) {
                    data class Edge(val cursor: Cursor)
                }
            }
        }
    }

    @Nested
    inner class GetStatuses {
        private fun readStars(userId: Int, pagination: ForwardPagination): ReadStarsResponse {
            val data = executeGraphQlViaEngine(
                """
                query ReadStars(${"$"}first: Int, ${"$"}after: Cursor) {
                    readStars {
                        edges {
                            node {
                                statuses(first: ${"$"}first, after: ${"$"}after) {
                                    edges {
                                        cursor
                                    }
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("first" to pagination.first, "after" to pagination.after?.toString()),
                userId,
            ).data!!["readStars"] as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `Statuses must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), userIdList)
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            userIdList.forEach { MessageStatuses.create(it, messageId, MessageStatus.DELIVERED) }
            val statusIdList = MessageStatuses.readIdList(messageId)
            val index = 4
            val pagination = ForwardPagination(first = 3, after = statusIdList.elementAt(index))
            val actual = readStars(adminId, pagination).edges.flatMap { edge ->
                edge.node.statuses.edges.map { it.cursor }
            }
            assertEquals(statusIdList.slice(index + 1..index + pagination.first!!).toList(), actual)
        }
    }
}
