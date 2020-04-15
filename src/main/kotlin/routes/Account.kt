package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Contacts
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Routing.routeAccount() {
    route("account") {
        authenticate {
            deleteAccount()
            readAccount()
            updateAccount()
        }
        createAccount()
    }
}

private fun Route.deleteAccount() {
    delete {
        Auth.deleteUser(call.userId)
        Contacts.deleteUserEntries(call.userId)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun Route.readAccount() {
    get {
        val username = Auth.findUserById(call.userId).username
        val user = Auth.findUserByUsername(username)
        call.respond(with(user) { AccountInfo(id, username, email, firstName, lastName) })
    }
}

private fun Route.updateAccount() {
    patch {
        val user = call.receive<AccountUpdate>()
        when {
            wantsTakenUsername(call.userId, user.username) ->
                call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.USERNAME_TAKEN))
            wantsTakenEmail(call.userId, user.email) ->
                call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.EMAIL_TAKEN))
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

private fun Route.createAccount() {
    post {
        val user = call.receive<NewAccount>()
        when {
            Auth.usernameExists(user.username) ->
                call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.USERNAME_TAKEN))
            Auth.emailExists(user.email) ->
                call.respond(HttpStatusCode.BadRequest, InvalidAccount(InvalidAccountReason.EMAIL_TAKEN))
            else -> {
                Auth.createUser(user)
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}