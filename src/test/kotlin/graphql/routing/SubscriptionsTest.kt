package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.buildTokenSet
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.graphql.operations.ACCOUNTS_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import io.ktor.http.cio.websocket.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
            val token = createVerifiedUsers(1)[0].accessToken
            executeGraphQlSubscriptionViaWebSocket(
                path = "accounts-subscription",
                GraphQlRequest(query, operationName = operationName),
                token
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
            val userId = createVerifiedUsers(1)[0].info.id
            val token = buildTokenSet(userId).accessToken
            Users.update(userId, AccountUpdate(emailAddress = "new.address@example.com"))
            executeGraphQlSubscriptionViaWebSocket(
                path = "messages-subscription",
                GraphQlRequest(subscribeToMessagesQuery),
                token
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
            request = GraphQlRequest(subscribeToAccountsQuery),
            accessToken = accessToken,
            callback = callback
        )
    }

    @Nested
    inner class Subscribe {
        @Test
        fun `Recreating a subscription mustn't cause duplicate notifications from the previous connection`() {
            val (owner, user) = createVerifiedUsers(2)
            subscribeToAccounts(owner.accessToken) {}
            subscribeToAccounts(owner.accessToken) { incoming ->
                parseFrameData<CreatedSubscription>(incoming)
                Contacts.create(owner.info.id, setOf(user.info.id))
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
