package com.neelkamath.omniChat.test.graphql.api.queries

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.Chat
import com.neelkamath.omniChat.GroupChat
import com.neelkamath.omniChat.PrivateChat
import com.neelkamath.omniChat.jsonMapper

/** Converts the [chat] to a type-safe [Chat]. The [chat] [Map] must include the `__typename` field. */
fun convertChat(chat: Any?): Chat {
    chat as Map<*, *>
    return when (chat["__typename"]) {
        "PrivateChat" -> jsonMapper.convertValue<PrivateChat>(chat)
        "GroupChat" -> jsonMapper.convertValue<GroupChat>(chat)
        else -> throw IllegalArgumentException("THe chat ($chat) was neither a PrivateChat nor a GroupChat.")
    }
}