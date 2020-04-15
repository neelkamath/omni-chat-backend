package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.ChatType
import com.neelkamath.omniChat.Chats
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.searchChats() {
    get("chats-search") {
        val query = call.parameters["query"]!!
        val privateChats = PrivateChats.search(call.userId, query).map { Chat(ChatType.PRIVATE, id = it) }
        val groupChats = GroupChats.search(call.userId, query).map { Chat(ChatType.GROUP, id = it) }
        call.respond(Chats(privateChats + groupChats))
    }
}