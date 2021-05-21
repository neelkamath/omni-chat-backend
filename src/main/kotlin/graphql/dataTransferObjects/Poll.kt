@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.PollMessageOptions
import com.neelkamath.omniChatBackend.db.tables.PollMessages
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class Poll(private val messageId: Int) {
    fun getTitle(): MessageText = PollMessages.readTitle(messageId)

    fun getOptions(): List<PollOption> = PollMessageOptions.readOptionIdList(messageId).map(::PollOption)
}
