package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Db
import com.neelkamath.omniChat.db.PrivateChatClears
import org.jetbrains.exposed.sql.select

/** Returns a [Boolean] [List] indicating whether the user who deleted the [chatId] was the creator of the chat. */
fun PrivateChatClears.read(chatId: Int): List<Boolean> = Db.transact {
    select { PrivateChatClears.chatId eq chatId }.map { it[isCreator] }
}