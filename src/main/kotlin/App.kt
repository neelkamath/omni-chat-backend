package com.neelkamath.omnichat

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*

fun Application.main() {
    install(CallLogging)
    install(ContentNegotiation) {
        gson {}
    }
    routing {
        get("health_check") { call.respond(HttpStatusCode.NoContent) }
    }
}