package com.neelkamath.omniChat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.routing.*
import graphql.schema.DataFetchingEnvironment
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
val ApplicationCall.userId: Int? get() = authentication.principal<JWTPrincipal>()?.payload?.subject?.toInt()

/** The user's ID on authenticated calls, and `null` otherwise. */
val DataFetchingEnvironment.userId: Int? get() = getContext()

fun Application.main() {
    setUpAuth()
    setUpDb()
    install(CORS) {
        anyHost()
        header(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
    }
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
    install(WebSockets) { pingPeriod = Duration.ofMinutes(1) }
    install(Authentication) {
        jwt {
            verifier(buildVerifier())
            validate { if (Users.exists(it.payload.subject.toInt())) JWTPrincipal(it.payload) else null }
        }
    }
    routing {
        routeHealthCheck(this)
        routeProfilePic(this)
        routeGroupChatPic(this)
        routeGraphQlQueriesAndMutations(this)
        routeGraphQlSubscriptions(this)
    }
}