package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.ContactsData
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Routing.routeUser() {
    route("user") {
        authenticate {
            delete()
            get()
            patch()
        }
        post()
    }
}

private fun Route.delete() {
    delete {
        Auth.deleteUser(call.userId)
        ContactsData.deleteUserEntries(call.userId)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.get() {
    get {
        val authorizedUsername = Auth.findUserById(call.userId).username
        val details =
            with(Auth.findUserByUsername(authorizedUsername)) { UserDetails(id, username, email, firstName, lastName) }
        call.respond(details)
    }
}

private fun Route.patch() {
    patch {
        val user = call.receive<User>()
        if (user.login?.username != null &&
            Auth.findUserById(call.userId).username != user.login.username &&
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
        if (Auth.usernameExists(user.login!!.username!!))
            call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.USERNAME_TAKEN))
        else {
            Auth.createUser(user)
            call.respond(HttpStatusCode.Created)
        }
    }
}