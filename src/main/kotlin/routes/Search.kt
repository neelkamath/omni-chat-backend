package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.UserPublicInfo
import com.neelkamath.omniChat.UserPublicInfoList
import com.neelkamath.omniChat.UserSearchQuery
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get

fun Routing.routeUserSearch() {
    get("user-search") {
        val query = parseQuery(call.parameters)
        call.respond(if (query.hasNoFilters()) HttpStatusCode.BadRequest else queryUsers(query))
    }
}

private fun parseQuery(parameters: Parameters): UserSearchQuery =
    UserSearchQuery(parameters["username"], parameters["first_name"], parameters["last_name"], parameters["email"])

private fun queryUsers(query: UserSearchQuery): UserPublicInfoList = UserPublicInfoList(
    Auth.searchUsers(query).map {
        val userId = Auth.findUserByUsername(it.username).id
        UserPublicInfo(userId, it.username, it.email, it.firstName, it.lastName)
    }
)