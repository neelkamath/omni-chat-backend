package com.neelkamath.omniChat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.routing.routeGraphQlQueriesAndMutations
import com.neelkamath.omniChat.routing.routeGraphQlSubscriptions
import com.neelkamath.omniChat.routing.routeHealthCheck
import com.neelkamath.omniChat.routing.routeProfilePic
import graphql.schema.DataFetchingEnvironment
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import java.time.Duration

/** Project-wide Jackson config. */
val objectMapper: ObjectMapper = jacksonObjectMapper()
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .findAndRegisterModules()

/** The user's ID on authenticated calls, and `null` otherwise. */
val ApplicationCall.userId: String? get() = authentication.principal<JWTPrincipal>()?.payload?.subject

/** The user's ID on authenticated calls, and `null` otherwise. */
val DataFetchingEnvironment.userId: String? get() = getContext()

fun Application.main() {
    setUpAuth()
    setUpDb()
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
    install(WebSockets) { pingPeriod = Duration.ofMinutes(1) }
    install(Authentication) {
        jwt {
            verifier(buildVerifier())
            validate { if (userIdExists(it.payload.subject)) JWTPrincipal(it.payload) else null }
        }
    }
    routing {
        routeHealthCheck(this)
        routeProfilePic(this)
        routeGraphQlQueriesAndMutations(this)
        routeGraphQlSubscriptions(this)
    }
}