package com.neelkamath.omniChatBackend.restApi

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

fun routeHealthCheck(routing: Routing): Unit = with(routing) {
    get("health-check") { call.respond(HttpStatusCode.NoContent) }
}
