package com.neelkamath.omniChatBackend.graphql.routing

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.tables.Contacts
import com.neelkamath.omniChatBackend.db.tables.Users
import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

typealias SubscriptionCallback = suspend (incoming: ReceiveChannel<Frame>) -> Unit

private data class EventData(val __typename: String)

/**
 * Opens a WebSocket connection on the URI's [path] (e.g., `"messages-subscription"`), sends the GraphQL subscription
 * [request], and has the [callback] [ReceiveChannel] and [SendChannel].
 */
inline fun executeGraphQlSubscriptionViaWebSocket(
    path: String,
    request: GraphQlRequest,
    accessToken: String? = null,
    crossinline callback: SubscriptionCallback,
): Unit = withTestApplication(Application::main) {
    handleWebSocketConversation(path) { incoming, outgoing ->
        if (accessToken != null) outgoing.send(Frame.Text(accessToken))
        launch(Dispatchers.IO) {
            val json = testingObjectMapper.writeValueAsString(request)
            outgoing.send(Frame.Text(json))
        }.join()
        callback(incoming)
    }
}

/** Returns the next [Frame.Text] (parsed as a [T]) so that you needn't deal with [Frame.Ping]s, etc. */
suspend inline fun <reified T> parseFrameData(channel: ReceiveChannel<Frame>): T {
    for (frame in channel)
        if (frame is Frame.Text) {
            val response = testingObjectMapper.readValue<GraphQlResponse>(frame.readText()).data!!
            return testingObjectMapper.convertValue(response.values.first()!!)
        }
    throw Exception("There was no text frame to be read.")
}

@ExtendWith(DbExtension::class)
class SubscriptionsTest {
    private val subscribeToMessagesQuery = """
        subscription SubscribeToMessages {
            subscribeToMessages {
                __typename
            }
        }
    """

    @Nested
    inner class RouteSubscription {
        private fun testOperationName(willSpecifyOperation: Boolean) {
            val query = """
                $subscribeToMessagesQuery
                
                subscription SubscribeToAccounts {
                    subscribeToAccounts {
                        __typename
                    }
                }
            """
            val operationName = "SubscribeToAccounts".takeIf { willSpecifyOperation }
            val token = createVerifiedUsers(1).first().accessToken
            executeGraphQlSubscriptionViaWebSocket(
                path = "accounts-subscription",
                GraphQlRequest(query, operationName = operationName),
                token,
            ) { incoming ->
                if (willSpecifyOperation)
                    assertEquals("CreatedSubscription", parseFrameData<EventData>(incoming).__typename)
                else assertEquals(FrameType.CLOSE, incoming.receive().frameType)
            }
        }

        @Test
        fun `The specified operation must be executed when there are multiple operations`(): Unit =
            testOperationName(willSpecifyOperation = true)

        @Test
        fun `An error must be returned when supplying multiple operations but not which to execute`(): Unit =
            testOperationName(willSpecifyOperation = false)

        @Test
        fun `A token from an account with an unverified email address mustn't work`() {
            val account = AccountInput(Username("u"), Password("p"), "u@example.com")
            Users.create(account)
            val userId = Users.readId(account.username)
            executeGraphQlSubscriptionViaWebSocket(
                path = "messages-subscription",
                GraphQlRequest(subscribeToMessagesQuery),
                buildTokenSet(userId).accessToken.value,
            ) { incoming -> assertEquals(FrameType.CLOSE, incoming.receive().frameType) }
        }
    }

    private fun subscribeToAccounts(accessToken: String? = null, callback: SubscriptionCallback) {
        val subscribeToAccountsQuery = """
            subscription SubscribeToAccounts {
                subscribeToAccounts {
                    __typename
                }
            }
        """
        executeGraphQlSubscriptionViaWebSocket(
            path = "accounts-subscription",
            GraphQlRequest(subscribeToAccountsQuery),
            accessToken,
            callback,
        )
    }

    @Nested
    inner class Subscribe {
        @Test
        fun `Recreating a subscription mustn't cause duplicate notifications from the previous connection`() {
            val (user, contact) = createVerifiedUsers(2)
            subscribeToAccounts(user.accessToken) {}
            subscribeToAccounts(user.accessToken) { incoming ->
                awaitBrokering()
                assertEquals("CreatedSubscription", parseFrameData<EventData>(incoming).__typename)
                Contacts.create(user.userId, contact.userId)
                awaitBrokering()
                assertEquals("NewContact", parseFrameData<EventData>(incoming).__typename)
                assertNull(incoming.poll())
            }
        }
    }

    @Nested
    inner class CloseWithError {
        @Test
        fun `The connection must be closed when calling an operation with an invalid token`(): Unit =
            subscribeToAccounts { incoming -> assertEquals(FrameType.CLOSE, incoming.receive().frameType) }
    }
}
