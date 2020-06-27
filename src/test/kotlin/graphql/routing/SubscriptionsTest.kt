package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.CreatedSubscription
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.subscriptions.CONTACT_UPDATES_QUERY
import com.neelkamath.omniChat.graphql.operations.subscriptions.receiveContactUpdates
import graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import graphql.operations.mutations.createContacts
import graphql.operations.subscriptions.operateGraphQlSubscription
import graphql.operations.subscriptions.parseFrameData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.FrameType

class SubscriptionsTest : FunSpec({
    context("routeSubscription(Routing, String, GraphQlSubscription)") {
        fun testOperationName(shouldSupplyOperationName: Boolean) {
            val query = """
                subscription MessageUpdates {
                    messageUpdates(chatId: 4) {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
                
                subscription ContactUpdates {
                    contactUpdates {
                        $CREATED_SUBSCRIPTION_FRAGMENT
                    }
                }
            """
            val operationName = if (shouldSupplyOperationName) "ContactUpdates" else null
            val token = createSignedInUsers(1)[0].accessToken
            operateGraphQlSubscription(
                uri = "contact-updates",
                request = GraphQlRequest(query, operationName = operationName),
                accessToken = token
            ) { incoming, _ ->
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

    context("closeWithError(DefaultWebSocketServerSession, ExecutionResult)") {
        test(
            """
            Given an operation requiring an access token,
            when calling the operation without a token,
            then the connection should be closed with the status code 1008
            """
        ) {
            operateGraphQlSubscription(
                uri = "contact-updates",
                request = GraphQlRequest(CONTACT_UPDATES_QUERY)
            ) { incoming, _ -> incoming.receive().frameType shouldBe FrameType.CLOSE }
        }
    }

    context("subscribe(DefaultWebSocketServerSession, GraphQlSubscription, ExecutionResult)") {
        test(
            """
            Given a client who recreated a GraphQL subscription,
            when they receive an event,
            then they should only receive it once because their previous connection's notifier should've been removed
            """
        ) {
            val (owner, user) = createSignedInUsers(2)
            receiveContactUpdates(owner.accessToken) { _, _ -> }
            receiveContactUpdates(owner.accessToken) { incoming, _ ->
                createContacts(owner.accessToken, listOf(user.info.id))
                incoming.poll().shouldNotBeNull()
                incoming.poll().shouldBeNull()
            }
        }
    }
})