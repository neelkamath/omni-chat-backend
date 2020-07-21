package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.TextMessage

/** Sends the [text] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(userId: Int, chatId: Int, text: TextMessage): Int {
    create(userId, chatId, text)
    return readIdList(chatId).last()
}