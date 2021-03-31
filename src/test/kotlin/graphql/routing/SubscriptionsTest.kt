package com.neelkamath.omniChat.graphql.routing

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.graphql.operations.ACCOUNTS_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private typealias SubscriptionCallback = suspend (incoming: ReceiveChannel<Frame>) -> Unit

/**
 * Opens a WebSocket connection on the URI's [path] (e.g., `"messages-subscription"`), sends the GraphQL subscription
 * [request], and has the [callback] [ReceiveChannel] and [SendChannel].
 */
private fun executeGraphQlSubscriptionViaWebSocket(
    path: String,
    request: GraphQlRequest,
    accessToken: String? = null,
    callback: SubscriptionCallback,
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

/**
 * Returns the next [Frame.Text] (parsed as a [T]) so that you needn't deal with [Frame.Ping]s, etc.
 *
 * It is assumed that there was only one operation (i.e., that the GraphQL response's `"data"` key contains only one
 * key-value pair).
 */
private suspend inline fun <reified T> parseFrameData(channel: ReceiveChannel<Frame>): T {
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
                $CREATED_SUBSCRIPTION_FRAGMENT
            }
        }
    """

    @Nested
    inner class RouteSubscription {
        private fun testOperationName(mustSupplyOperationName: Boolean) {
            val query = """
                $subscribeToMessagesQuery
                
                subscription SubscribeToAccounts {
                    subscribeToAccounts {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
            """
            val operationName = "SubscribeToAccounts".takeIf { mustSupplyOperationName }
            val token = createVerifiedUsers(1).first().accessToken
            executeGraphQlSubscriptionViaWebSocket(
                path = "accounts-subscription",
                GraphQlRequest(query, operationName = operationName),
                token,
            ) { incoming ->
                if (mustSupplyOperationName) parseFrameData<CreatedSubscription>(incoming)
                else assertEquals(FrameType.CLOSE, incoming.receive().frameType)
            }
        }

        @Test
        fun `The specified operation must be executed when there are multiple`() {
            testOperationName(mustSupplyOperationName = true)
        }

        @Test
        fun `An error must be returned when supplying multiple operations but not which to execute`() {
            testOperationName(mustSupplyOperationName = false)
        }

        @Test
        fun `A token from an account with an unverified email address mustn't work`() {
            val userId = createVerifiedUsers(1).first().info.id
            val token = buildTokenSet(userId).accessToken
            Users.update(userId, AccountUpdate(emailAddress = "new.address@example.com"))
            executeGraphQlSubscriptionViaWebSocket(
                path = "messages-subscription",
                GraphQlRequest(subscribeToMessagesQuery),
                token,
            ) { incoming -> assertEquals(FrameType.CLOSE, incoming.receive().frameType) }
        }
    }

    private fun subscribeToAccounts(accessToken: String? = null, callback: SubscriptionCallback) {
        val subscribeToAccountsQuery = """
            subscription SubscribeToAccounts {
                subscribeToAccounts {
                    $ACCOUNTS_SUBSCRIPTION_FRAGMENT
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
                parseFrameData<CreatedSubscription>(incoming)
                Contacts.create(user.info.id, contact.info.id)
                awaitBrokering()
                assertNotNull(incoming.poll())
                assertNull(incoming.poll())
            }
        }
    }

    @Nested
    inner class CloseWithError {
        @Test
        fun `The connection must be closed when calling an operation with an invalid token`() {
            subscribeToAccounts { incoming -> assertEquals(FrameType.CLOSE, incoming.receive().frameType) }
        }
    }
}
