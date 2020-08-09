package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.PollInput

fun Messages.create(userId: Int, chatId: Int, text: MessageText = MessageText("t")): Unit =
    createTextMessage(userId, chatId, text, contextMessageId = null)

/** Sends the [message] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(
    userId: Int,
    chatId: Int,
    message: MessageText = MessageText("t"),
    contextMessageId: Int? = null
): Int {
    createTextMessage(userId, chatId, message, contextMessageId)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: Mp3,
    contextMessageId: Int? = null
): Int {
    createAudioMessage(userId, chatId, message, contextMessageId)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: CaptionedPic,
    contextMessageId: Int? = null
): Int {
    createPicMessage(userId, chatId, message, contextMessageId)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: PollInput,
    contextMessageId: Int? = null
): Int {
    createPollMessage(userId, chatId, message, contextMessageId)
    return readIdList(chatId).last()
}