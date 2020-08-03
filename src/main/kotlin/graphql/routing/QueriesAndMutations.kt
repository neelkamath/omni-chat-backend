package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.graphql.UnauthorizedException
import com.neelkamath.omniChat.graphql.engine.buildExecutionInput
import com.neelkamath.omniChat.graphql.engine.buildSpecification
import com.neelkamath.omniChat.graphql.engine.graphQl
import graphql.execution.UnknownOperationException
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post

/**
 * Adds the HTTP POST `/query-or-mutation` endpoint to the [context], which deals with every GraphQL query and mutation.
 */
fun routeGraphQlQueriesAndMutations(context: Routing): Unit = with(context) {
    authenticate(optional = true) {
        post("query-or-mutation") {
            val builder = buildExecutionInput(call.receive(), call)
            try {
                val result = graphQl.execute(builder)
                call.respond(buildSpecification(result))
            } catch (_: UnauthorizedException) {
                call.respond(HttpStatusCode.Unauthorized)
            } catch (_: UnknownOperationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "You have supplied multiple GraphQL operations but haven't specified which one to execute."
                )
            }
        }
    }
}