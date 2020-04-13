package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.User
import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.UserSearchQuery
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.searchUsers() {
    get("user-search") {
        val query = parseQuery(call.parameters)
        if (query.hasNoFilters()) call.respond(HttpStatusCode.BadRequest)
        else {
            val userIdList = Auth.searchUsers(query).map { it.id }.toSet()
            call.respond(UserIdList(userIdList))
        }
    }
}

private fun parseQuery(parameters: Parameters): UserSearchQuery =
    UserSearchQuery(parameters["username"], parameters["first_name"], parameters["last_name"], parameters["email"])

fun Routing.readUser() {
    get("user") {
        val userId = call.parameters["user_id"]!!
        if (Auth.userIdExists(userId))
            with(Auth.findUserById(userId)) { call.respond(User(username, email, firstName, lastName)) }
        else call.respond(HttpStatusCode.BadRequest)
    }
}