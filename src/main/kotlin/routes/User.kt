package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Contacts
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
        Contacts.deleteUserEntries(call.userId)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.get() {
    get {
        val username = Auth.findUserById(call.userId).username
        val user = Auth.findUserByUsername(username)
        call.respond(with(user) { UserInfo(id, username, email, firstName, lastName) })
    }
}

private fun Route.patch() {
    patch {
        val user = call.receive<UserUpdate>()
        when {
            wantsTakenUsername(call.userId, user.username) ->
                call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.USERNAME_TAKEN))
            wantsTakenEmail(call.userId, user.email) ->
                call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.EMAIL_TAKEN))
            else -> {
                Auth.updateUser(call.userId, user)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun wantsTakenUsername(userId: String, wantedUsername: String?): Boolean =
    wantedUsername != null &&
            Auth.findUserById(userId).username != wantedUsername &&
            Auth.isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: String, wantedEmail: String?): Boolean =
    wantedEmail != null && Auth.findUserById(userId).email != wantedEmail && Auth.emailExists(wantedEmail)

private fun Route.post() {
    post {
        val user = call.receive<NewUser>()
        when {
            Auth.usernameExists(user.username) ->
                call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.USERNAME_TAKEN))
            Auth.emailExists(user.email) ->
                call.respond(HttpStatusCode.BadRequest, InvalidUser(InvalidUserReason.EMAIL_TAKEN))
            else -> {
                Auth.createUser(user)
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}

fun Routing.routeEmailVerification() {
    get("email-verification") {
        val email = call.parameters["email"]!!
        if (Auth.emailExists(email)) {
            Auth.sendEmailVerification(email)
            call.respond(HttpStatusCode.NoContent)
        } else call.respond(HttpStatusCode.BadRequest)
    }
}

fun Routing.routePasswordReset() {
    get("password-reset") {
        val email = call.parameters["email"]!!
        if (Auth.emailExists(email)) {
            Auth.resetPassword(email)
            call.respond(HttpStatusCode.NoContent)
        } else call.respond(HttpStatusCode.BadRequest)
    }
}