package com.neelkamath.omniChatBackend.graphql.dataTransferObjects

import com.neelkamath.omniChatBackend.graphql.routing.Placeholder

class NonexistingOption : SetPollVoteResult {
    fun getPlaceholder() = Placeholder
}
