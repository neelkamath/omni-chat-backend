package com.neelkamath.omniChatBackend.graphql.routing

import com.neelkamath.omniChatBackend.graphql.engine.UnauthorizedException
import com.neelkamath.omniChatBackend.graphql.engine.buildExecutionInput
import com.neelkamath.omniChatBackend.graphql.engine.buildSpecification
import com.neelkamath.omniChatBackend.graphql.engine.graphQl
import com.neelkamath.omniChatBackend.userId
import graphql.execution.UnknownOperationException
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

/**
 * Adds the HTTP POST `/query-or-mutation` endpoint to the [context], which deals with every GraphQL query and mutation.
 */
fun routeGraphQlQueriesAndMutations(context: Routing): Unit = with(context) {
    authenticate(optional = true) {
        post("query-or-mutation") {
            val builder = buildExecutionInput(call.receive(), call.userId)
            try {
                val result = graphQl.execute(builder)
                call.respond(buildSpecification(result))
            } catch (_: UnauthorizedException) {
                call.respond(HttpStatusCode.Unauthorized)
            } catch (_: UnknownOperationException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "You have supplied multiple GraphQL operations but haven't specified which one to execute.",
                )
            }
        }
    }
}
