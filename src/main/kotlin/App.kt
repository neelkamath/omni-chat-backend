package com.neelkamath.omniChat

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.neelkamath.omniChat.routes.routeHealthCheck
import com.neelkamath.omniChat.routes.routeJwt
import com.neelkamath.omniChat.routes.routeRefreshJwt
import com.neelkamath.omniChat.routes.routeUser
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.routing.routing
import org.keycloak.representations.idm.UserRepresentation

/** Project-wide Gson config. */
val gson: Gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    .create()

/** On authenticated calls, this will be the callee's [UserRepresentation.id]. */
val ApplicationCall.userId get(): String = authentication.principal<JWTPrincipal>()!!.payload.subject

fun Application.main() {
    Auth.setUp()
    DB.setUp()
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, GsonConverter(gson)) }
    install(Authentication) {
        jwt {
            realm = Auth.realmName
            verifier(Jwt.buildVerifier())
            validate { if (it.payload.audience.contains(Jwt.audience)) JWTPrincipal(it.payload) else null }
        }
    }
    routing {
        routeHealthCheck()
        routeJwt()
        routeRefreshJwt()
        routeUser()
    }
}