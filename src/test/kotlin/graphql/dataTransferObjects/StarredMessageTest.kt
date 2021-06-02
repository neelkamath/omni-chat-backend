package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class StarredMessageTest {
    private data class ReadStarsResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val statuses: List<Edge>) {
                data class Edge(val cursor: Cursor)
            }
        }
    }

    @Nested
    inner class GetStatuses {
        private fun readStars(userId: Int, pagination: ForwardPagination): ReadStarsResponse {
            val data = executeGraphQlViaEngine(
                """
                query ReadStars(${"$"}first: Int, ${"$"}after: Cursor) {
                    readStars() {
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
            ).data as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `Statuses must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map {
                Messages.message(adminId, chatId).also { Stargazers.create(adminId, it) }
            }
            val index = 4
            val pagination = ForwardPagination(first = 3, after = messageIdList.elementAt(index))
            val actual = readStars(adminId, pagination).edges.flatMap { edge ->
                edge.node.statuses.map { it.cursor }
            }
            assertEquals(messageIdList.slice(index + 1..index + pagination.first!!), actual)
        }
    }
}
