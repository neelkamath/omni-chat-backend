@file:Suppress("unused")

package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.db.tables.PollMessageOptions
import com.neelkamath.omniChatBackend.db.tables.PollMessageVotes
import com.neelkamath.omniChatBackend.graphql.routing.MessageText

class PollOption(private val optionId: Int) {
    fun getOption(): MessageText = PollMessageOptions.readOption(optionId)

    fun getVotes(): List<Account> = PollMessageVotes.read(optionId).map(::Account)
}
