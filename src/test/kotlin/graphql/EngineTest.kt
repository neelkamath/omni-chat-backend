package com.neelkamath.omniChat.graphql

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.api.mutations.createMessage
import com.neelkamath.omniChat.graphql.api.subscriptions.parseFrameData
import com.neelkamath.omniChat.graphql.api.subscriptions.receiveMessageUpdates
import io.kotest.core.spec.style.FunSpec
import java.time.Instant

class EngineTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
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
}