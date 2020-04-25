package com.neelkamath.omniChat

import com.auth0.jwt.JWT
import com.neelkamath.omniChat.graphql.ClientException
import com.neelkamath.omniChat.graphql.buildSchema
import graphql.ExceptionWhileDataFetching
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.GraphQLError
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post

private val graphQl: GraphQL = GraphQL.newGraphQL(buildSchema()).build()

fun Routing.checkHealth() {
    get("health-check") { call.respond(HttpStatusCode.NoContent) }
}

fun Routing.refreshJwt() {
    post("jwt-refresh") {
        val refreshToken = call.receiveParameters()["refresh_token"]!!
        val userId = JWT.decode(refreshToken).subject
        val authToken = Jwt.buildAuthToken(userId, Auth.refreshToken(refreshToken))
        call.respond(authToken)
    }
}

fun Routing.routeGraphQl() {
    authenticate(optional = true) {
        post("graphql") {
            val request = call.receive<GraphQlRequest>()
            val builder = ExecutionInput.Builder()
                .query(request.query)
                .variables(request.variables ?: mapOf())
                .operationName(request.operationName)
                .context(call.authentication.principal<JWTPrincipal>()?.payload?.subject)
            call.respond(executeGraphQl(builder))
        }
    }
}

/** Returns the GraphQL response. */
private fun executeGraphQl(builder: ExecutionInput.Builder): Map<String, Any> {
    val result = graphQl.execute(builder)
    return result.toSpecification()
        .mapValues { if (it.key == "errors") result.errors.map(::maskError) else it.value }
        .filterNot {
            (it.key == "data" && it.value == null) || (it.key == "errors" && (it.value as List<*>).isEmpty())
        }
}

/** Masks the [error], and returns its [GraphQLError.toSpecification]. */
private fun maskError(error: GraphQLError): Map<String, Any> {
    val result = error.toSpecification()
    result["message"] = when {
        error is ExceptionWhileDataFetching && error.exception is ClientException -> error.exception.message
        error is ExceptionWhileDataFetching -> "Internal server error"
        else -> error.message
    }
    return result
}