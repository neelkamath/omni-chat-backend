package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Routing.routeUser() {
    route("user") {
        authenticate {
            get()
            patch()
        }
        post()
    }
}

private fun Route.get() {
    get {
        val authorizedUsername = Auth.getUsername(call.userId)
        val details = with(Auth.findUser(authorizedUsername)) { UserDetails(username, email, firstName, lastName) }
        call.respond(details)
    }
}

private fun Route.patch() {
    patch {
        val user = call.receive<User>()
        if (user.login?.username != null &&
            Auth.getUsername(call.userId) != user.login.username &&
            Auth.isUsernameTaken(user.login.username)
        ) {
            call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.USERNAME_TAKEN))
        } else {
            Auth.updateUser(call.userId, user)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.post() {
    post {
        val user = call.receive<User>()
        if (Auth.userExists(user.login!!.username!!))
            call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.USERNAME_TAKEN))
        else {
            Auth.createUser(user)
            call.respond(HttpStatusCode.Created)
        }
    }
}