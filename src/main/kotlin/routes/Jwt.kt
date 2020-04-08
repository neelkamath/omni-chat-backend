package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post

fun Routing.routeJwt() {
    post("jwt") {
        val login = call.receive<Login>()
        when {
            !Auth.userExists(login.username!!) ->
                call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.NONEXISTENT_USER))
            !Auth.findUser(login.username).isEmailVerified ->
                call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.EMAIL_NOT_VERIFIED))
            else -> {
                val token = Auth.getToken(login)
                if (token == null)
                    call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.INCORRECT_PASSWORD))
                else call.respond(Jwt.buildAuthToken(Auth.getUserId(login.username), token))
            }
        }
    }
}