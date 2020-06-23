package com.neelkamath.omniChat.graphql.api

import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.main
import com.neelkamath.omniChat.objectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

class ApiTest : FunSpec({
    context("routeGraphQl(Routing)") {
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
    }
})