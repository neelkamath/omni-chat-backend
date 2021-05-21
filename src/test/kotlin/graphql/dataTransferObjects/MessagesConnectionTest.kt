package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.Cursor
import com.neelkamath.omniChatBackend.testingObjectMapper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(DbExtension::class)
class MessagesConnectionTest {
    private data class ReadChatResponse(val messages: Messages) {
        data class Messages(val pageInfo: PageInfo) {
            data class PageInfo(val startCursor: Cursor?, val endCursor: Cursor?)
        }
    }

    @Nested
    inner class GetPageInfo {
        private fun getPageInfo(userId: Int, chatId: Int): ReadChatResponse.Messages.PageInfo {
            val data = executeGraphQlViaEngine(
                """
                query ReadChat(${"$"}id: Int!) {
                    readChat(id: ${"$"}id) {
                        ... on Chat {
                            messages {
                                pageInfo {
                                    startCursor
                                    endCursor
                                }
                            }
                        }
                    }
                }
                """,
                mapOf("id" to chatId),
                userId,
            ).data!!["readChat"] as Map<*, *>
            return testingObjectMapper.convertValue<ReadChatResponse>(data).messages.pageInfo
        }

        @Test
        fun `The start and end cursors must point to the first and last items of the specified chat respectively`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val privateChatId = PrivateChats.create(user1Id, user2Id)
            val (privateChatMessage1Id, privateChatMessage2Id) = (1..2).map { Messages.message(user1Id, privateChatId) }
            val groupChatId = GroupChats.create(adminIdList = listOf(user1Id))
            val (groupChatMessage1Id, groupChatMessage2Id) = (1..2).map { Messages.message(user1Id, groupChatId) }
            // Query cursors after each chat's messages have been created to verify the correct chat's cursor is gotten.
            val (privateChatStartCursor, privateChatEndCursor) = getPageInfo(user1Id, privateChatId)
            assertEquals(privateChatMessage1Id, privateChatStartCursor)
            assertEquals(privateChatMessage2Id, privateChatEndCursor)
            val (groupChatStartCursor, groupChatEndCursor) = getPageInfo(user1Id, groupChatId)
            assertEquals(groupChatMessage1Id, groupChatStartCursor)
            assertEquals(groupChatMessage2Id, groupChatEndCursor)
        }

        @Test
        fun `If there are zero items, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            val (startCursor, endCursor) = getPageInfo(adminId, chatId)
            assertNull(startCursor)
            assertNull(endCursor)
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val (startCursor, endCursor) = getPageInfo(adminId, chatId)
            assertEquals(messageId, startCursor)
            assertEquals(messageId, endCursor)
        }
    }
}
