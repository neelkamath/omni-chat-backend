package com.neelkamath.omniChat.graphql.engine

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.tables.GroupChatDescription
import com.neelkamath.omniChat.db.tables.GroupChatTitle
import com.neelkamath.omniChat.db.tables.TextMessage
import com.neelkamath.omniChat.graphql.createSignedInUsers
import com.neelkamath.omniChat.graphql.operations.mutations.createGroupChat
import com.neelkamath.omniChat.graphql.operations.mutations.createMessage
import com.neelkamath.omniChat.graphql.operations.subscriptions.parseFrameData
import com.neelkamath.omniChat.graphql.operations.subscriptions.subscribeToMessages
import io.kotest.core.spec.style.FunSpec
import java.time.Instant

class DateTimeCoercingTest : FunSpec({
    /** Returns a GraphQL `DateTime` scalar. */
    fun getScalar(): String {
        val token = createSignedInUsers(1)[0].accessToken
        val chat = NewGroupChat(GroupChatTitle("Title"), GroupChatDescription(""))
        val chatId = createGroupChat(token, chat)
        var sent: String? = null
        subscribeToMessages(token, chatId) { incoming ->
            createMessage(token, chatId, TextMessage("text"))
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