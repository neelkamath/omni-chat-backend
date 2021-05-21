@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.MessageStatuses
import com.neelkamath.omniChatBackend.graphql.routing.MessageStatus
import java.time.LocalDateTime

class MessageDateTimeStatus(private val statusId: Int) {
    fun getUser(): Account = Account(MessageStatuses.readUserId(statusId))

    fun getDateTime(): LocalDateTime = MessageStatuses.readDateTime(statusId)

    fun getStatus(): MessageStatus = MessageStatuses.readStatus(statusId)
}
