@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.MessageText

/** The [messageId]'s [action] triggered by the [userId]. */
class TriggeredAction(private val messageId: Int, private val action: MessageText, private val userId: Int) :
    MessagesSubscription {

    fun getMessageId(): Int = messageId

    fun getAction(): MessageText = action

    fun getTriggeredBy(): Account = Account(userId)
}
