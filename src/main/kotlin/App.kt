package com.neelkamath.omnichat

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.main() {
    initDb()
    install(CallLogging)
    install(ContentNegotiation) {
        gson {}
    }
    routing {
        get("health_check") { call.respond(HttpStatusCode.NoContent) }
    }
}