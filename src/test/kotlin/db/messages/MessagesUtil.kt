package com.neelkamath.omniChat.db.messages

import com.neelkamath.omniChat.db.transact
import org.jetbrains.exposed.sql.selectAll

/** Sends the [text] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(chatId: Int, userId: String, text: String): Int {
    create(chatId, userId, text)
    return readIdList(chatId).last()
}

/** Returns the number of messages in every chat. */
fun Messages.count(): Long = transact { selectAll().count() }