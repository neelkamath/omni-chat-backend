package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class MessageTest {
    private data class ReadMessageResponse(val isBookmarked: Boolean)

    private fun readMessage(userId: Int?, messageId: Int, pagination: ForwardPagination? = null): ReadMessageResponse {
        val data = executeGraphQlViaEngine(
            """
            query ReadMessage(${"$"}messageId: Int!) {
                readMessage(messageId: ${"$"}messageId) {
                    ... on Message {
                        isBookmarked
                    }
                }
            }
            """,
            mapOf("messageId" to messageId, "first" to pagination?.first, "after" to pagination?.after?.toString()),
            userId,
        ).data!!["readMessage"] as Map<*, *>
        return testingObjectMapper.convertValue(data)
    }

    @Nested
    inner class GetIsBookmarked {
        private fun assertIsBookmarked(isBookmarked: Boolean) {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            Bookmarks.create(adminId, messageId)
            val actual = readMessage(if (isBookmarked) adminId else null, messageId).isBookmarked
            assertEquals(isBookmarked, actual)
        }

        @Test
        fun `A bookmarked message must be stated as such when the user requests it`(): Unit = assertIsBookmarked(true)

        @Test
        fun `A message bookmarked by a user must not be bookmarked when read by an anonymous user`(): Unit =
            assertIsBookmarked(false)
    }
}
