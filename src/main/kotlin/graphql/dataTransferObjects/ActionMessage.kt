@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

class ActionMessage(override val id: Int) : Message {
    fun getActionableMessage(): ActionableMessage = ActionableMessage(id)
}
