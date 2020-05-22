package com.neelkamath.omniChat.test.graphql.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.GraphQlResponse
import com.neelkamath.omniChat.jsonMapper
import com.neelkamath.omniChat.main
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

/**
 * Returns the response body after [query]ing the GraphQL API with a JSON request body of the [variables].
 *
 * This only works for GraphQL queries and mutations.
 */
fun operateQueryOrMutation(
    query: String,
    variables: Map<String, Any?>? = null,
    accessToken: String? = null
): GraphQlResponse {
    val call = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "graphql") {
            if (accessToken != null) addHeader(HttpHeaders.Authorization, "Bearer $accessToken")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            val body = mapOf("query" to query, "variables" to variables)
            setBody(jsonMapper.writeValueAsString(body))
        }
    }
    return jsonMapper.readValue(call.response.content!!)
}