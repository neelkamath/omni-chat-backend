package com.neelkamath.omniChat.routing

import com.neelkamath.omniChat.CreatedSubscription
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.tables.Contacts
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.DELETED_CONTACT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.NEW_CONTACT_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.UPDATED_CONTACT_FRAGMENT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType
import kotlinx.coroutines.time.delay
import java.time.Duration

class GraphQlSubscriptionsTest : FunSpec({
    context("routeSubscription(Routing, String, GraphQlSubscription)") {
        fun testOperationName(shouldSupplyOperationName: Boolean) {
            val query = """
                subscription SubscribeToMessages {
                    subscribeToMessages {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
                
                subscription SubscribeToContacts {
                    subscribeToContacts {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
            """
            val operationName = if (shouldSupplyOperationName) "SubscribeToContacts" else null
            val token = createVerifiedUsers(1)[0].accessToken
            executeGraphQlSubscriptionViaWebSocket(
                uri = "contacts-subscription",
                request = GraphQlRequest(query, operationName = operationName),
                accessToken = token
            ) { incoming ->
                if (shouldSupplyOperationName) parseFrameData<CreatedSubscription>(incoming)
                else incoming.receive().frameType shouldBe FrameType.CLOSE
            }
        }

        test(
            """
            Given multiple operations, 
            when an operation name is supplied, 
            then the specified operation should be executed
            """
        ) { testOperationName(shouldSupplyOperationName = true) }

        test(
            """
            Given multiple operations, 
            when no operation name is supplied, 
            then an error should be returned
            """
        ) { testOperationName(shouldSupplyOperationName = false) }
    }

    fun subscribeToContacts(accessToken: String? = null, callback: SubscriptionCallback) {
        val subscribeToContactsQuery = """
            subscription SubscribeToContacts {
                subscribeToContacts {
                    $CREATED_SUBSCRIPTION_FRAGMENT
                    $NEW_CONTACT_FRAGMENT
                    $UPDATED_CONTACT_FRAGMENT
                    $DELETED_CONTACT_FRAGMENT
                }
            }
        """
        executeGraphQlSubscriptionViaWebSocket(
            uri = "contacts-subscription",
            request = GraphQlRequest(subscribeToContactsQuery),
            accessToken = accessToken,
            callback = callback
        )
    }

    context("subscribe(DefaultWebSocketServerSession, GraphQlSubscription, ExecutionResult)") {
        test(
            """
            Given a client who recreated a GraphQL subscription,
            when they receive an event,
            then they should only receive it once because their previous notifier should've been removed
            """
        ) {
            val (owner, user) = createVerifiedUsers(2)
            subscribeToContacts(owner.accessToken) {}
            subscribeToContacts(owner.accessToken) { incoming ->
                parseFrameData<CreatedSubscription>(incoming)
                Contacts.create(owner.info.id, setOf(user.info.id))
                delay(Duration.ofNanos(1)) // Await event emission.
                incoming.poll().shouldNotBeNull()
                incoming.poll().shouldBeNull()
            }
        }
    }

    context("closeWithError(DefaultWebSocketServerSession, ExecutionResult)") {
        test(
            """
            Given an operation requiring an access token,
            when calling the operation without a token,
            then the connection should be closed
            """
        ) {
            subscribeToContacts { incoming -> incoming.receive().frameType shouldBe FrameType.CLOSE }
        }
    }
})