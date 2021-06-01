package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
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
    private data class ReadMessageResponse(val hasStar: Boolean)

    @Nested
    inner class GetHasStar {
        private fun assertHasStar(hasStar: Boolean) {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val data = executeGraphQlViaEngine(
                """
                query ReadMessage(${"$"}messageId: Int!) {
                    readMessage(messageId: ${"$"}messageId) {
                        hasStar
                    }
                }
                """,
                mapOf("messageId" to messageId),
                if (hasStar) adminId else null,
            ).data!!["readMessage"] as Map<*, *>
            val actual = testingObjectMapper.convertValue<ReadMessageResponse>(data).hasStar
            assertEquals(hasStar, actual)
        }

        @Test
        fun `A starred message must be stated as such when the user requests it`(): Unit = assertHasStar(true)

        @Test
        fun `A message starred by a user must not be starred when read by an anonymous user`(): Unit =
            assertHasStar(false)
    }
}
