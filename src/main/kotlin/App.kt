package com.neelkamath.omniChat

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neelkamath.omniChat.db.DB
import com.neelkamath.omniChat.routes.*
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.routing.Routing
import io.ktor.routing.routing
import org.keycloak.representations.idm.UserRepresentation

/** Project-wide Gson config. */
val gson: Gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // ISO 8601 format.
    .create()

/** On authenticated calls, this will be the callee's [UserRepresentation.id]. */
val ApplicationCall.userId get(): String = authentication.principal<JWTPrincipal>()!!.payload.subject

fun Application.main() {
    Auth.setUp()
    DB.setUp()
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, GsonConverter(gson)) }
    install(Authentication) { jwt() }
    routing { route() }
}

/**
 * Whether the app is running in a testing environment.
 *
 * An example use case is to enable code which sends emails only in the development and production environments.
 */
fun isTestEnvironment(): Boolean = System.getenv("IS_TEST_ENVIRONMENT") == "1"

private fun Authentication.Configuration.jwt(): Unit = jwt {
    realm = Auth.realmName
    verifier(Jwt.buildVerifier())
    validate {
        if (it.payload.audience.contains(Jwt.audience) && Auth.userIdExists(it.payload.subject))
            JWTPrincipal(it.payload)
        else null
    }
}

/*
Name the functions the same as their <operationId>s in the OpenAPI spec. If there is a common <io.ktor.routing.route>
function for the endpoints (e.g., <routeAccount()>), name it according to the format "route<URL>".
 */
private fun Routing.route() {
    checkHealth()
    requestJwt()
    refreshJwt()
    routeAccount()
    searchUsers()
    verifyEmail()
    resetPassword()
    readUser()
    authenticate {
        routeContacts()
        createGroupChat()
    }
}