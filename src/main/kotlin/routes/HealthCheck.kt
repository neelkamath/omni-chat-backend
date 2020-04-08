package com.neelkamath.omniChat.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.routeHealthCheck() {
    get("health_check") { call.respond(HttpStatusCode.NoContent) }
}