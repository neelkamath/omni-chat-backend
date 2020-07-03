package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.CreatedSubscription
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.TextMessage
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.CREATED_SUBSCRIPTION_FRAGMENT
import com.neelkamath.omniChat.graphql.operations.mutations.createContacts
import com.neelkamath.omniChat.graphql.operations.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.subscriptions.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.FrameType

class SubscriptionsTest : FunSpec({
    context("routeSubscription(Routing, String, GraphQlSubscription)") {
        fun testOperationName(shouldSupplyOperationName: Boolean) {
            val query = """
                subscription SubscribeToMessages {
                    subscribeToMessages(chatId: 4) {
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
            val token = createSignedInUsers(1)[0].accessToken
            operateGraphQlSubscription(
                uri = "subscribe-to-contacts",
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

    context("closeWithError(DefaultWebSocketServerSession, ExecutionResult)") {
        test(
            """
            Given an operation requiring an access token,
            when calling the operation without a token,
            then the connection should be closed with the status code 1008
            """
        ) {
            operateGraphQlSubscription(
                uri = "subscribe-to-contacts",
                request = GraphQlRequest(SUBSCRIBE_TO_CONTACTS_QUERY)
            ) { incoming -> incoming.receive().frameType shouldBe FrameType.CLOSE }
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
            subscribeToContacts(owner.accessToken) { }
            subscribeToContacts(owner.accessToken) { incoming ->
                createContacts(owner.accessToken, listOf(user.info.id))
                incoming.poll().shouldNotBeNull()
                incoming.poll().shouldBeNull()
            }
        }

        test(
            """
            Given a client who failed to ping during the ping period,
            when they recreate the connection,
            then the previous connection's subscription shouldn't be active so duplicate notifications aren't sent 
            """
        ) {
            val token = createSignedInUsers(1)[0].accessToken
            val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
            val chatId = createGroupChat(token, chat)
            subscribeToMessages(token, chatId) { incoming -> for (frame in incoming) if (frame is Frame.Close) break }
            subscribeToMessages(token, chatId) { incoming ->
                createMessage(token, chatId, TextMessage("text"))
                incoming.poll().shouldNotBeNull()
                incoming.poll().shouldBeNull()
            }
        }
    }
})