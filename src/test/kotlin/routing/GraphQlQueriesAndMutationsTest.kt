package com.neelkamath.omniChat.routing

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.GroupChats
import com.neelkamath.omniChat.db.tables.create
import com.neelkamath.omniChat.graphql.operations.READ_ACCOUNT_QUERY
import com.neelkamath.omniChat.graphql.operations.UPDATE_GROUP_CHAT_TITLE_QUERY
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

class GraphQlQueriesAndMutationsTest : FunSpec({
    context("routeGraphQlQueriesAndMutations(Routing)") {
        test("The GraphQL engine should be queried via the HTTP interface") {
            val user = createVerifiedUsers(1)[0]
            val response = executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = user.accessToken).content!!
            val data = objectMapper.readValue<GraphQlResponse>(response).data!!["readAccount"] as Map<*, *>
            objectMapper.convertValue<Account>(data) shouldBe user.info
        }

        fun testOperationName(shouldSupplyOperationName: Boolean) {
            withTestApplication(Application::main) {
                handleRequest(HttpMethod.Post, "query-or-mutation") {
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
            executeGraphQlViaHttp(READ_ACCOUNT_QUERY)
                .shouldHaveUnauthorizedStatus()
        }

        test(
            """
            Given an operation requiring an access token,
            when supplying an invalid token to the operation,
            then an HTTP status code of 401 should be received
            """
        ) {
            executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = "invalid token")
                .shouldHaveUnauthorizedStatus()
        }

        test(
            """
            Given an operation which can only be called by particular users,
            when supplying the access token of a user who lacks the required permissions,
            then an HTTP status code of 401 should be received
            """
        ) {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_TITLE_QUERY,
                mapOf("chatId" to chatId, "title" to "T"),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }
})