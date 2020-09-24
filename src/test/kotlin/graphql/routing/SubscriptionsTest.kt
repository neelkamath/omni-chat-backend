package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.buildOnetimeToken
import com.neelkamath.omniChat.buildTokenSet
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.db.tables.OnetimeTokens
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.db.tables.read
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
        private fun testOperationName(shouldSupplyOperationName: Boolean) {
            val query = """
                $subscribeToMessagesQuery
                
                subscription SubscribeToAccounts {
                    subscribeToAccounts {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
            """
            val operationName = "SubscribeToAccounts".takeIf { shouldSupplyOperationName }
            val token = createVerifiedUsers(1)[0].accessToken
            executeGraphQlSubscriptionViaWebSocket(
                path = "accounts-subscription",
                GraphQlRequest(query, operationName = operationName),
                token
            ) { incoming ->
                if (shouldSupplyOperationName) parseFrameData<CreatedSubscription>(incoming)
                else assertEquals(FrameType.CLOSE, incoming.receive().frameType)
            }
        }

        @Test
        fun `The specified operation should be executed when there are multiple`() {
            testOperationName(shouldSupplyOperationName = true)
        }

        @Test
        fun `An error should be returned when supplying multiple operations but not which to execute`() {
            testOperationName(shouldSupplyOperationName = false)
        }

        private fun testOnetimeToken(shouldUseValidToken: Boolean) {
            val token = createVerifiedUsers(1)[0].info.id.let(::buildOnetimeToken)
            repeat(if (shouldUseValidToken) 1 else 2) { repetition ->
                executeGraphQlSubscriptionViaWebSocket(
                    path = "messages-subscription",
                    GraphQlRequest(subscribeToMessagesQuery),
                    token
                ) { incoming ->
                    if (repetition == 0) parseFrameData<CreatedSubscription>(incoming)
                    else assertEquals(FrameType.CLOSE, incoming.receive().frameType)
                }
            }
            assertEquals(0, OnetimeTokens.read().size)
        }

        @Test
        fun `The connection shouldn't be closed if an unused onetime token was supplied`() {
            testOnetimeToken(shouldUseValidToken = true)
        }

        @Test
        fun `The connection should be closed if a used onetime token was supplied`() {
            testOnetimeToken(shouldUseValidToken = false)
        }

        @Test
        fun `Using a non-onetime token should work`() {
            val userId = createVerifiedUsers(1)[0].info.id
            executeGraphQlSubscriptionViaWebSocket(
                path = "messages-subscription",
                GraphQlRequest(subscribeToMessagesQuery),
                buildOnetimeToken(userId)
            ) { incoming -> parseFrameData<CreatedSubscription>(incoming) }
        }

        @Test
        fun `A token from an account with an unverified email address shouldn't work`() {
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
        fun `Recreating a subscription shouldn't cause duplicate notifications from the previous connection`() {
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
        fun `The connection should be closed when calling an operation with an invalid token`() {
            subscribeToAccounts { incoming -> assertEquals(FrameType.CLOSE, incoming.receive().frameType) }
        }
    }
}
