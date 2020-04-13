package com.neelkamath.omniChat.routes

import com.auth0.jwt.JWT
import com.neelkamath.omniChat.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post

fun Routing.requestJwt() {
    post("jwt-request") {
        val login = call.receive<Login>()
        when {
            !Auth.usernameExists(login.username) ->
                call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.NONEXISTENT_USER))
            !Auth.findUserByUsername(login.username).isEmailVerified ->
                call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.EMAIL_NOT_VERIFIED))
            else -> {
                val token = Auth.getToken(login)
                if (token == null)
                    call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.INCORRECT_PASSWORD))
                else {
                    val userId = Auth.findUserByUsername(login.username).id
                    call.respond(Jwt.buildAuthToken(userId, token))
                }
            }
        }
    }
}

fun Routing.refreshJwt() {
    post("jwt-refresh") {
        val token = call.receiveParameters()["refresh_token"]!!
        val userId = JWT.decode(token).subject
        call.respond(Jwt.buildAuthToken(userId, Auth.refreshToken(token)))
    }
}