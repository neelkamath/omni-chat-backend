package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.db.tables.message
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class ChatTest {
    private data class ReadChatResponse(val messages: Messages) {
        data class Messages(val edges: List<Edge>) {
            data class Edge(val node: Node) {
                data class Node(val messageId: Int)
            }
        }
    }

    @Nested
    inner class GetMessages {
        @Test
        fun `Messages must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val last = 3
            val index = 7
            val data = executeGraphQlViaEngine(
                """
                query ReadChat(${"$"}id: Int!, ${"$"}last: Int, ${"$"}before: Cursor) {
                    readChat(id: ${"$"}id) {
                        ... on GroupChat {
                            messages(last: ${"$"}last, before: ${"$"}before) {
                                edges {
                                    node {
                                        messageId
                                    }
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("id" to chatId, "last" to last, "before" to messageIdList[index].toString()),
                adminId,
            ).data!!["readChat"] as Map<*, *>
            val actual = testingObjectMapper.convertValue<ReadChatResponse>(data).messages.edges.map { it.node.messageId }
            assertEquals(messageIdList.subList(index - last, index), actual)
        }
    }
}
