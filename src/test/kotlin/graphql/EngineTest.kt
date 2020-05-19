package com.neelkamath.omniChat.test.graphql

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.test.AuthListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.test.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.test.graphql.api.subscriptions.operateMessageUpdates
import com.neelkamath.omniChat.test.graphql.api.subscriptions.parseFrameData
import io.kotest.core.spec.style.FunSpec
import java.time.Instant

class EngineTest : FunSpec({
    listener(AuthListener())

    /** Returns a GraphQL `DateTime` scalar. */
    fun getScalar(): String {
        val token = createVerifiedUsers(1)[0].accessToken
        val chatId = createGroupChat(NewGroupChat("Title"), token)
        var sent = ""
        operateMessageUpdates(chatId, token) { incoming, _ ->
            createMessage(chatId, "text", token)
            val message = parseFrameData<Map<String, Any>>(incoming)
            val dateTimes = message["dateTimes"] as Map<*, *>
            sent = dateTimes["sent"] as String
        }
        return sent
    }

    test("DateTime scalars should be ISO 8601-compliant") {
        Instant.parse(getScalar()) // Successfully parsing verifies the format.
    }
})