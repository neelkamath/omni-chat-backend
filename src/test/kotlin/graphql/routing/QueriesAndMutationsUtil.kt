package com.neelkamath.omniChat.graphql.routing

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.main
import com.neelkamath.omniChat.objectMapper
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

/** [executeGraphQlViaHttp] wrapper which parses the [TestApplicationResponse.content] as a [Map]. */
fun readGraphQlHttpResponse(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): Map<String, Any> = executeGraphQlViaHttp(query, variables, accessToken).content!!.let(objectMapper::readValue)

/**
 * Executes GraphQL queries and mutations via the HTTP interface. The [variables] are for the query, which is the
 * GraphQL doc.
 *
 * @see [readGraphQlHttpResponse]
 * @see [executeGraphQlViaEngine]
 */
fun executeGraphQlViaHttp(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "query-or-mutation") {
        accessToken?.let { addHeader(HttpHeaders.Authorization, "Bearer $it") }
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val body = GraphQlRequest(query, variables)
        setBody(objectMapper.writeValueAsString(body))
    }.response
}