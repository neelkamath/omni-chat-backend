@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class ActionMessage(override val id: Int) : Message, ReadMessageResult {
    fun getActionableMessage(): ActionableMessage = ActionableMessage(id)
}
