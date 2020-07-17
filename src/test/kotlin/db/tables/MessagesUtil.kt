package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.TextMessage
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/** Sends the [text] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(chatId: Int, userId: Int, text: TextMessage): Int {
    create(chatId, userId, text)
    return readIdList(chatId).last()
}

/** Returns the number of messages in every chat. */
fun Messages.count(): Long = transaction { selectAll().count() }