package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.UserIdList
import com.neelkamath.omniChat.UserSearchQuery
import io.ktor.application.call
import io.ktor.http.Parameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.searchUsers() {
    get("user-search") {
        val userIdList = Auth.searchUsers(parseQuery(call.parameters)).map { it.id }.toSet()
        call.respond(UserIdList(userIdList))
    }
}

private fun parseQuery(parameters: Parameters): UserSearchQuery =
    UserSearchQuery(parameters["username"], parameters["first_name"], parameters["last_name"], parameters["email"])