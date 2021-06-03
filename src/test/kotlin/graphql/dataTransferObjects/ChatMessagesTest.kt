package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.BackwardPagination
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
class ChatMessagesTest {
    private data class SearchMessagesResponse(val edges: List<Edge>) {
        data class Edge(val node: Node) {
            data class Node(val chat: Chat) {
                data class Chat(val messages: Messages) {
                    data class Messages(val edges: List<Edge>) {
                        data class Edge(val node: Node) {
                            data class Node(val messageId: Int)
                        }
                    }
                }
            }
        }
    }

    @Nested
    inner class GetMessages {
        private fun getMessages(userId: Int, pagination: BackwardPagination? = null): List<Int> {
            val data = executeGraphQlViaEngine(
                """
                query SearchMessages(${"$"}query: String!, ${"$"}last: Int, ${"$"}before: Cursor) {
                    searchMessages(query: ${"$"}query) {
                        edges {
                            node {
                                chat {
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
                    }
                }
                """,
                mapOf("query" to "", "last" to pagination?.last, "before" to pagination?.before?.toString()),
                userId,
            ).data!!["searchMessages"] as Map<*, *>
            return testingObjectMapper.convertValue<SearchMessagesResponse>(data).edges.flatMap { (node) ->
                node.chat.messages.edges.map { it.node.messageId }
            }
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            assertEquals(messageIdList, getMessages(adminId))
        }

        @Test
        fun `The number of items specified by the limit must be returned from before the cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val last = 3
            val index = 7
            val actual = getMessages(adminId, BackwardPagination(last, before = messageIdList[index]))
            assertEquals(messageIdList.subList(index - last, index), actual)
        }

        @Test
        fun `The number of items specified by the limit from the last item must be retrieved when there's no cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val last = 3
            val actual = getMessages(adminId, BackwardPagination(last))
            assertEquals(messageIdList.takeLast(last), actual)
        }

        @Test
        fun `Every item before the cursor must be retrieved when there's no limit`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val index = 7
            val actual = getMessages(adminId, BackwardPagination(before = messageIdList[index]))
            assertEquals(messageIdList.take(index), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the first item's cursor`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val actual = getMessages(adminId, BackwardPagination(before = messageIdList[0])).size
            assertEquals(0, actual)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the last three items before item 6, then items 2, 3, and 5 must be retrieved`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            Messages.delete(messageIdList[3])
            val actual = getMessages(adminId, BackwardPagination(last = 3, before = messageIdList[5]))
            assertEquals(listOf(messageIdList[1], messageIdList[2], messageIdList[4]), actual)
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageIdList = (1..10).map { Messages.message(adminId, chatId) }
            val index = 4
            Messages.delete(messageIdList[index])
            val actual = getMessages(adminId, BackwardPagination(before = messageIdList[index]))
            assertEquals(messageIdList.take(index), actual)
        }
    }
}
