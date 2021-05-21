@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class NewActionMessage(override val id: Int) : MessagesSubscription, NewMessage {
    fun getActionableMessage(): ActionableMessage = ActionableMessage(id)
}
