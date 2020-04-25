package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.Db
import graphql.schema.DataFetchingEnvironment
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing

/**
 * Whether the app is running in the testing environment.
 *
 * An example use case of this is to enable code that sends emails only in development and production.
 */
val isTestingEnvironment: Boolean = System.getenv("IS_TESTING_ENVIRONMENT") == "1"

/** On authenticated calls, this will be the user's ID. */
val DataFetchingEnvironment.userId get(): String = getContext()

fun Application.main() {
    Auth.setUp()
    Db.setUp()
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(jacksonObjectMapper)) }
    install(Authentication) { jwt() }
    routing {
        routeGraphQl()
        checkHealth()
        refreshJwt()
    }
}

private fun Authentication.Configuration.jwt(): Unit = jwt {
    realm = Auth.realmName
    verifier(Jwt.buildVerifier())
    validate {
        if (it.payload.audience.contains(Jwt.audience) && Auth.userIdExists(it.payload.subject))
            JWTPrincipal(it.payload)
        else null
    }
}