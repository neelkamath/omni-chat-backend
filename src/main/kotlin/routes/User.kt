package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.User
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.readUser() {
    get("user") {
        val userId = call.parameters["user_id"]!!
        if (Auth.userIdExists(userId))
            with(Auth.findUserById(userId)) { call.respond(User(username, email, firstName, lastName)) }
        else call.respond(HttpStatusCode.BadRequest)
    }
}