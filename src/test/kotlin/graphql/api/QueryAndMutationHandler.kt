package com.neelkamath.omniChat.test.graphql.api

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.main
import com.neelkamath.omniChat.objectMapper
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

/** Similar to [queryOrMutate] but parses the [GraphQlResponse] as well. */
fun operateQueryOrMutation(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): GraphQlResponse {
    val response = queryOrMutate(query, variables, accessToken)
    return objectMapper.convertValue(response)
}

/**
 * Returns the response body after [query]ing (a GraphQL document for a query or mutation) the GraphQL API with a JSON
 * request body of the [variables].
 */
fun queryOrMutate(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): Map<String, Any> {
    val call = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "graphql") {
            if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            val body = mapOf("query" to query, "variables" to variables)
            setBody(objectMapper.writeValueAsString(body))
        }
    }
    return objectMapper.readValue(call.response.content!!)
}