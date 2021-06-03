@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class NewActionMessage(override val id: Int) : MessagesSubscription, NewMessage, ChatMessagesSubscription {
    fun getActionableMessage(): ActionableMessage = ActionableMessage(id)
}
