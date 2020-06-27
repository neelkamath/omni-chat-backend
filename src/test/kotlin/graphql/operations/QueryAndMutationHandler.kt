package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.GraphQlRequest
import com.neelkamath.omniChat.GraphQlResponse
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

/**
 * Executes GraphQL queries and mutations.
 *
 * @param[query] GraphQL document.
 * @param[variables] GraphQL variables for the [query].
 * @see [callGraphQlQueryOrMutation]
 */
fun operateGraphQlQueryOrMutation(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): GraphQlResponse {
    val response = callGraphQlQueryOrMutation(query, variables, accessToken)
    return objectMapper.convertValue(response)
}

/**
 * Executes GraphQL queries and mutations.
 *
 * @param[query] GraphQL document.
 * @param[variables] GraphQL variables for the [query].
 * @return GraphQL response as a [Map].
 * @see [operateGraphQlQueryOrMutation]
 */
fun callGraphQlQueryOrMutation(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): Map<String, Any> =
    requestGraphQlQueryOrMutation(query, variables, accessToken).content!!.let(objectMapper::readValue)

/**
 * Executes a GraphQL queries and mutations.
 *
 * @param[query] GraphQL document.
 * @param[variables] GraphQL variables for the [query].
 * @see [callGraphQlQueryOrMutation]
 * @see [operateGraphQlQueryOrMutation]
 */
fun requestGraphQlQueryOrMutation(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "graphql") {
        if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val body = GraphQlRequest(query, variables)
        setBody(objectMapper.writeValueAsString(body))
    }
}.response