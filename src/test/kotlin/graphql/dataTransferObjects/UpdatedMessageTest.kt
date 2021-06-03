package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.routing.*
import com.neelkamath.omniChatBackend.slice
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class UpdatedMessageTest {
    private data class CreatedSubscriptionResponse(val __typename: String)

    private data class UpdatedMessageResponse(val statuses: Statuses) {
        data class Statuses(val edges: List<Edge>) {
            data class Edge(val cursor: Cursor)
        }
    }

    @Nested
    inner class GetStatuses {
        @Test
        fun `Statuses must be paginated`() {
            val admin = createVerifiedUsers(1).first()
            val userIdList = createVerifiedUsers(10).map { it.userId }
            val chatId = GroupChats.create(setOf(admin.userId), userIdList)
            val messageId = Messages.message(admin.userId, chatId)
            userIdList.dropLast(1).forEach { MessageStatuses.create(it, messageId, MessageStatus.DELIVERED) }
            val statusIdList = MessageStatuses.readIdList(messageId)
            val first = 3
            val index = 4
            val after = statusIdList.elementAt(index)
            executeGraphQlSubscriptionViaWebSocket(
                path = "messages-subscription",
                GraphQlRequest(
                    """
                    subscription SubscribeToMessages(${"$"}first: Int, ${"$"}after: Cursor) {
                        subscribeToMessages {
                            __typename
                            ... on UpdatedMessage {
                                statuses(first: ${"$"}first, after: ${"$"}after) {
                                    edges {
                                        cursor
                                    }
                                }
                            }
                        }
                    }
                    """,
                    mapOf("first" to first, "after" to after.toString()),
                ),
                admin.accessToken,
            ) { incoming ->
                assertEquals("CreatedSubscription", parseFrameData<CreatedSubscriptionResponse>(incoming).__typename)
                MessageStatuses.create(userIdList.last(), messageId, MessageStatus.DELIVERED)
                awaitBrokering()
                val actual = parseFrameData<UpdatedMessageResponse>(incoming).statuses.edges.map { it.cursor }
                assertEquals(statusIdList.slice(index + 1..index + first).toList(), actual)
            }
        }
    }
}
