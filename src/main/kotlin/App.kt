package com.neelkamath.omniChat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.graphql.routing.routeGraphQlSubscriptions
import com.neelkamath.omniChat.graphql.routing.routeQueriesAndMutations
import graphql.schema.DataFetchingEnvironment
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import java.time.Duration

/** Project-wide Jackson config. */
val objectMapper: ObjectMapper = jacksonObjectMapper()
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .findAndRegisterModules()

/** On authenticated calls, this will be the user's ID. */
val ApplicationCall.userId: String? get() = authentication.principal<JWTPrincipal>()?.payload?.subject

/** On authenticated calls, this will be the user's ID. */
val DataFetchingEnvironment.userId: String? get() = getContext()

fun Application.main() {
    setUpAuth()
    setUpDb()
    installFeatures(this)
    routing { route(this) }
}

/** [Application.install]s features. */
private fun installFeatures(context: Application): Unit = with(context) {
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
    install(WebSockets) { pingPeriod = Duration.ofMinutes(1) }
    install(Authentication) { buildJwtConfig(this) }
}

private fun buildJwtConfig(context: Authentication.Configuration): Unit = with(context) {
    jwt {
        verifier(buildVerifier())
        validate { if (userIdExists(it.payload.subject)) JWTPrincipal(it.payload) else null }
    }
}

/** Registers every route. */
private fun route(context: Routing): Unit = with(context) {
    get("health-check") { call.respond(HttpStatusCode.NoContent) }
    routeQueriesAndMutations(context)
    routeGraphQlSubscriptions(context)
}