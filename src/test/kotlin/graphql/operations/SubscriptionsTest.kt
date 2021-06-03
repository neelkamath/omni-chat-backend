package com.neelkamath.omniChatBackend.graphql.operations

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.tables.GroupChats
import com.neelkamath.omniChatBackend.db.tables.Messages
import com.neelkamath.omniChatBackend.db.tables.create
import com.neelkamath.omniChatBackend.db.tables.message
import com.neelkamath.omniChatBackend.graphql.routing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class SubscriptionsTest {
    private data class EventData(val __typename: String)

    @Nested
    inner class SubscribeToChat {
        private fun testSubscription(usesPublicChat: Boolean) {
            val adminId = createVerifiedUsers(1).first().userId
            val publicity = if (usesPublicChat) GroupChatPublicity.PUBLIC else GroupChatPublicity.NOT_INVITABLE
            val chatId = GroupChats.create(setOf(adminId), publicity = publicity)
            val query = """
                subscription SubscribeToChatMessages(${"$"}chatId: Int!) {
                    subscribeToChatMessages(chatId: ${"$"}chatId) {
                        __typename
                    }
                }
            """
            executeGraphQlSubscriptionViaWebSocket(
                path = "chat-messages-subscription",
                GraphQlRequest(query, mapOf("chatId" to chatId)),
            ) { incoming ->
                assertEquals("CreatedSubscription", parseFrameData<EventData>(incoming).__typename)
                Messages.message(adminId, chatId)
                awaitBrokering()
                val type = parseFrameData<EventData>(incoming).__typename
                assertEquals(if (usesPublicChat) "NewTextMessage" else "InvalidChatId", type)
            }
        }

        @Test
        fun `Attempting to subscribe to a chat other than a public chat must fail`(): Unit =
            testSubscription(usesPublicChat = false)

        @Test
        fun `Attempting to subscribe to a public chat must succeed`(): Unit = testSubscription(usesPublicChat = true)
    }
}
