package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.subscriptions.receiveMessageUpdates
import graphql.operations.mutations.createGroupChat
import graphql.operations.mutations.createMessage
import graphql.operations.subscriptions.parseFrameData
import io.kotest.core.spec.style.FunSpec
import java.time.Instant

class DateTimeCoercingTest : FunSpec({
    /** Returns a GraphQL `DateTime` scalar. */
    fun getScalar(): String {
        val token = createSignedInUsers(1)[0].accessToken
        val chatId = createGroupChat(token, NewGroupChat("Title"))
        var sent: String? = null
        receiveMessageUpdates(token, chatId) { incoming, _ ->
            createMessage(token, chatId, "text")
            val message = parseFrameData<Map<String, Any>>(incoming)
            val dateTimes = message["dateTimes"] as Map<*, *>
            sent = dateTimes["sent"] as String
        }
        return sent!!
    }

    test("DateTime scalars should be ISO 8601-compliant") {
        Instant.parse(getScalar()) // Successfully parsing verifies the format.
    }
})