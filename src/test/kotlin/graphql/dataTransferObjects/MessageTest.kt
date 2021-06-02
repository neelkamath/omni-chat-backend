package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import com.neelkamath.omniChatBackend.slice
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class MessageTest {
    private data class ReadMessageResponse(val hasStar: Boolean, val statuses: List<Edge>) {
        data class Edge(val cursor: Cursor)
    }

    private fun readMessage(userId: Int?, messageId: Int, pagination: ForwardPagination? = null): ReadMessageResponse {
        val data = executeGraphQlViaEngine(
            """
            query ReadMessage(${"$"}messageId: Int!, ${"$"}first: Int, ${"$"}after: Cursor) {
                readMessage(messageId: ${"$"}messageId) {
                    statuses(first: ${"$"}first, after: ${"$"}after) {
                        edges {
                            cursor
                        }
                    }
                    hasStar
                }
            }
            """,
            mapOf("messageId" to messageId, "first" to pagination?.first, "after" to pagination?.after),
            userId,
        ).data!!["readMessage"] as Map<*, *>
        return testingObjectMapper.convertValue(data)
    }

    @Nested
    inner class GetStatuses {
        @Test
        fun `Statuses must be paginated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), userIdList)
            val messageId = Messages.message(adminId, chatId)
            userIdList.forEach { MessageStatuses.create(it, messageId, MessageStatus.DELIVERED) }
            val statusIdList = MessageStatuses.readIdList(messageId)
            val index = 4
            val pagination = ForwardPagination(first = 3, after = statusIdList.elementAt(index))
            val actual = readMessage(adminId, messageId, pagination).statuses.map { it.cursor }
            assertEquals(statusIdList.slice(index + 1..index + pagination.first!!).toList(), actual)
        }
    }

    @Nested
    inner class GetHasStar {
        private fun assertHasStar(hasStar: Boolean) {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val actual = readMessage(if (hasStar) adminId else null, messageId).hasStar
            assertEquals(hasStar, actual)
        }

        @Test
        fun `A starred message must be stated as such when the user requests it`(): Unit = assertHasStar(true)

        @Test
        fun `A message starred by a user must not be starred when read by an anonymous user`(): Unit =
            assertHasStar(false)
    }
}
