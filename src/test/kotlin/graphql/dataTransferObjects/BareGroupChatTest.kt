package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class BareGroupChatTest {
    private data class ReadChatResponse(val users: Users) {
        data class Users(val edges: List<Edge>) {
            data class Edge(val node: Node) {
                data class Node(val userId: Int)
            }
        }
    }

    @Nested
    inner class GetUsers {
        @Test
        fun `Users must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId), userIdList)
            val first = 3
            val index = 4
            val data = executeGraphQlViaEngine(
                """
                query ReadChat(${"$"}id: Int!, ${"$"}first: Int, ${"$"}after: Cursor) {
                    readChat(id: ${"$"}id) {
                        ... on GroupChat {
                            users(first: ${"$"}first, after: ${"$"}after) {
                                edges {
                                    node {
                                        userId
                                    }
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("id" to chatId, "first" to first, "after" to userIdList[index].toString()),
                adminId,
            ).data!!["readChat"] as Map<*, *>
            val actual = testingObjectMapper.convertValue<ReadChatResponse>(data).users.edges.map { it.node.userId }
            assertEquals(userIdList.slice(index + 1..index + first), actual)
        }
    }
}
