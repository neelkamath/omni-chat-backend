package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.db.tables.Messages
import com.neelkamath.omniChat.db.tables.Users

fun AccountInput.toAccount(): Account =
    Account(Users.read(username).id, username, emailAddress, firstName, lastName, bio)

fun NewMessage.Companion.build(message: Message): NewMessage = object : NewMessage {
    override val chatId: Int = Messages.readChatFromMessage(message.messageId)
    override val messageId: Int = message.messageId
    override val sender: Account = message.sender
    override val dateTimes: MessageDateTimes = message.dateTimes
    override val context: MessageContext = message.context
    override val isForwarded: Boolean = message.isForwarded
}

fun ActionMessageInput.toActionableMessage(): ActionableMessage = ActionableMessage(text, actions)
