package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

object NonexistingOption : SetPollVoteResult {
    fun getPlaceholder() = Placeholder
}
