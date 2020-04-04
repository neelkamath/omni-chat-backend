package com.neelkamath.omniChat

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing

/** App-wide Gson config. */
val gson: Gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

fun Application.main() {
    setUpAuth()
    initDb()
    install(CallLogging)
    install(ContentNegotiation) { register(ContentType.Application.Json, GsonConverter(gson)) }
    routing {
        get("health_check") { call.respond(HttpStatusCode.NoContent) }
        post("user") {
            val user = call.receive<User>()
            if (userExists(user.username)) call.respond(HttpStatusCode.BadRequest)
            else {
                createUser(user)
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}