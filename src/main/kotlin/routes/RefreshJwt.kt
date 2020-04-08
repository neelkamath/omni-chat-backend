package com.neelkamath.omniChat.routes

import com.auth0.jwt.JWT
import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.Jwt
import io.ktor.application.call
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.post

fun Routing.routeRefreshJwt() {
    post("jwt_refresh") {
        val token = call.receiveParameters()["refresh_token"]!!
        val userId = JWT.decode(token).subject
        call.respond(Jwt.buildAuthToken(userId, Auth.refreshToken(token)))
    }
}