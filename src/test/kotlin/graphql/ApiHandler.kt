package com.neelkamath.omniChat.test.graphql

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.jacksonObjectMapper
import com.neelkamath.omniChat.main
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

data class GraphQlResponse(val data: Map<String, Any>?, val errors: List<GraphQlResponseError>?)

data class GraphQlResponseError(val message: String)

/** Returns the response body after [query]ing the GraphQL API with a JSON request body of the [variables]. */
fun operateGraphQl(query: String, variables: Map<String, Any?>? = null, jwt: String? = null): GraphQlResponse {
    val call = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "graphql") {
            if (jwt != null) addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            val body = mapOf("query" to query, "variables" to variables)
            setBody(jacksonObjectMapper.writeValueAsString(body))
        }
    }
    return jacksonObjectMapper.readValue(call.response.content!!)
}