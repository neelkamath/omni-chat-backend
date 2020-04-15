package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.ChatId
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post

fun Route.createPrivateChat() {
    post("private-chat") {
        val invitedUserId = call.parameters["user_id"]!!
        if (Auth.userIdExists(invitedUserId) && invitedUserId != call.userId) {
            val id = PrivateChats.create(call.userId, invitedUserId)
            call.respond(ChatId(id))
        } else call.respond(HttpStatusCode.BadRequest)
    }
}