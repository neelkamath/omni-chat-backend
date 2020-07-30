package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.TextMessage

fun Messages.create(userId: Int, chatId: Int, text: TextMessage = TextMessage("t")): Unit =
    create(userId, chatId, text, contextMessageId = null)

/** Sends the [text] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(
    userId: Int,
    chatId: Int,
    text: TextMessage = TextMessage("t"),
    contextMessageId: Int? = null
): Int {
    create(userId, chatId, text, contextMessageId)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    audio: Mp3,
    contextMessageId: Int? = null
): Int {
    create(userId, chatId, audio, contextMessageId)
    return readIdList(chatId).last()
}