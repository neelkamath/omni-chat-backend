package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.db.Audio
import com.neelkamath.omniChat.graphql.routing.ActionMessageInput
import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.PollInput

fun Messages.create(
    userId: Int,
    chatId: Int,
    text: MessageText = MessageText("t"),
    contextMessageId: Int? = null,
    isForwarded: Boolean = false
): Unit = createTextMessage(userId, chatId, text, contextMessageId, isForwarded)

/** Sends the [message] in the [chatId] from the [userId], and returns the message's ID. */
fun Messages.message(
    userId: Int,
    chatId: Int,
    message: MessageText = MessageText("t"),
    contextMessageId: Int? = null,
    isForwarded: Boolean = false
): Int {
    createTextMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: Audio,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false
): Int {
    createAudioMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: CaptionedPic,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false
): Int {
    createPicMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: PollInput,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createPollMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}

fun Messages.message(
    userId: Int,
    chatId: Int,
    message: ActionMessageInput,
    contextMessageId: Int? = null,
    isForwarded: Boolean = false,
): Int {
    createActionMessage(userId, chatId, message, contextMessageId, isForwarded)
    return readIdList(chatId).last()
}
