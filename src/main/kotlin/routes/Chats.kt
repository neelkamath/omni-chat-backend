package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.ChatType
import com.neelkamath.omniChat.Chats
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.PrivateChatClears
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.userId
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.readChats() {
    get("chats") {
        val groupChats = GroupChats.read(call.userId).map { Chat(ChatType.GROUP, it.id) }
        val privateChats = PrivateChats.read(call.userId).map { Chat(ChatType.PRIVATE, it.id) }.filter {
            val isCreator = PrivateChats.isCreator(it.id, call.userId)
            !PrivateChatClears.hasCleared(isCreator, it.id)
        }
        call.respond(Chats(groupChats + privateChats))
    }
}