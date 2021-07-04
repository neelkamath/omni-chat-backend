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
class BookmarkedMessagesConnectionTest {
    private data class ReadBookmarksResponse(val pageInfo: PageInfo) {
        data class PageInfo(val startCursor: Cursor?, val endCursor: Cursor?)
    }

    @Nested
    inner class GetPageInfo {
        private fun getPageInfo(userId: Int): ReadBookmarksResponse.PageInfo {
            val data = executeGraphQlViaEngine(
                """
                query ReadBookmarks {
                    readBookmarks {
                        pageInfo {
                            startCursor
                            endCursor
                        }
                    }
                }
                """,
                userId = userId,
            ).data!!["readBookmarks"] as Map<*, *>
            return testingObjectMapper.convertValue<ReadBookmarksResponse>(data).pageInfo
        }

        @Test
        fun `If there are zero items, the page info must indicate such`() {
            val userId = createVerifiedUsers(1).first().userId
            val (startCursor, endCursor) = getPageInfo(userId)
            assertNull(startCursor)
            assertNull(endCursor)
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Bookmarks.create(adminId, messageId)
            val (startCursor, endCursor) = getPageInfo(adminId)
            assertEquals(messageId, startCursor)
            assertEquals(messageId, endCursor)
        }

        @Test
        fun `The start and end cursors must point to the first and last items respectively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map {
                Messages.message(adminId, chatId).also { Bookmarks.create(adminId, it) }
            }
            val (startCursor, endCursor) = getPageInfo(adminId)
            assertEquals(messageIdList[0], startCursor)
            assertEquals(messageIdList.last(), endCursor)
        }
    }
}
