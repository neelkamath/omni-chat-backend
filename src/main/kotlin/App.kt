package com.neelkamath.omniChat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.neelkamath.omniChat.db.setUpDb
import com.neelkamath.omniChat.db.subscribeToMessageBroker
import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.graphql.routing.routeGraphQlQueriesAndMutations
import com.neelkamath.omniChat.graphql.routing.routeGraphQlSubscriptions
import com.neelkamath.omniChat.restApi.*
import graphql.schema.DataFetchingEnvironment
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.websocket.*
import java.time.Duration

/** Project-wide Jackson config. */
val objectMapper: ObjectMapper = jacksonObjectMapper()
    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    // Use <registerModule()> instead of <findAndRegisterModules()> because the latter doesn't work in the prod build.
    .registerModule(JavaTimeModule())

/** The user's ID on authenticated calls, and `null` otherwise. */
val ApplicationCall.userId: Int? get() = authentication.principal<JWTPrincipal>()?.payload?.subject?.toInt()

/** The user's ID on authenticated calls, and `null` otherwise. */
val DataFetchingEnvironment.userId: Int? get() = getContext()

fun Application.main() {
    setUpDb()
    subscribeToMessageBroker()
    install(CORS) {
        anyHost()
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
    }
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
    install(WebSockets) { pingPeriod = Duration.ofMinutes(1) }
    install(Authentication) {
        jwt {
            verifier(jwtVerifier)
            validate { credential ->
                val userId = credential.payload.subject.toInt()
                // It's possible the user updated their email address just after the token was created.
                if (Users.isExisting(userId) && Users.read(userId).hasVerifiedEmailAddress) JWTPrincipal(credential.payload)
                else null
            }
        }
    }
    routing {
        routeHealthCheck(this)
        routePicMessage(this)
        routeAudioMessage(this)
        routeVideoMessage(this)
        routeDocMessage(this)
        routeProfilePic(this)
        routeGroupChatPic(this)
        routeGraphQlQueriesAndMutations(this)
        routeGraphQlSubscriptions(this)
    }
}
