package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.mutations.UPDATE_GROUP_CHAT_QUERY
import com.neelkamath.omniChat.graphql.operations.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.operations.queries.READ_ACCOUNT_QUERY
import com.neelkamath.omniChat.graphql.operations.requestGraphQlQueryOrMutation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

class QueriesAndMutationsTest : FunSpec({
    context("routeQueriesAndMutations(Routing)") {
        fun testOperationName(shouldSupplyOperationName: Boolean) {
            withTestApplication(Application::main) {
                handleRequest(HttpMethod.Post, "graphql") {
                    val query = """
                        query IsUsernameTaken {
                            isUsernameTaken(username: "john_doe")
                        }
                        
                        query IsEmailAddressTaken {
                            isEmailAddressTaken(emailAddress: "john.doe@example.com")
                        }
                    """
                    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    val operationName = if (shouldSupplyOperationName) "IsEmailAddressTaken" else null
                    val body = GraphQlRequest(query, operationName = operationName)
                    setBody(objectMapper.writeValueAsString(body))
                }.response.status()!!.value shouldBe if (shouldSupplyOperationName) 200 else 400
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

        test("An HTTP status code of 401 should be received when a mandatory access token wasn't supplied") {
            requestGraphQlQueryOrMutation(READ_ACCOUNT_QUERY).shouldHaveUnauthorizedStatus()
        }

        test(
            """
            Given an operation requiring an access token,
            when supplying an invalid token to the operation,
            then an HTTP status code of 401 should be received
            """
        ) {
            requestGraphQlQueryOrMutation(READ_ACCOUNT_QUERY, accessToken = "invalid token")
                .shouldHaveUnauthorizedStatus()
        }

        test(
            """
            Given an operation which can only be called by particular users,
            when supplying the access token of a user who lacks the required permissions,
            then an HTTP status code of 401 should be received
            """
        ) {
            val (admin, user) = createSignedInUsers(2)
            val chatId = createGroupChat(admin.accessToken, buildNewGroupChat(user.info.id))
            val variables = mapOf("update" to GroupChatUpdate(chatId))
            requestGraphQlQueryOrMutation(UPDATE_GROUP_CHAT_QUERY, variables, user.accessToken)
                .shouldHaveUnauthorizedStatus()
        }
    }
})