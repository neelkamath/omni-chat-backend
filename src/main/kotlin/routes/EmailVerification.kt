package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.verifyEmail() {
    get("email-verification") {
        val email = call.parameters["email"]!!
        if (Auth.emailExists(email)) {
            Auth.sendEmailVerification(email)
            call.respond(HttpStatusCode.NoContent)
        } else call.respond(HttpStatusCode.BadRequest)
    }
}