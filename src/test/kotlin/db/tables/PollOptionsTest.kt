package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.graphql.routing.PollInput
import com.neelkamath.omniChatBackend.graphql.routing.PollOption
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class PollOptionsTest {
    @Nested
    inner class Read {
        @Test
        fun `Options must be read in the order they were created`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val options = listOf(MessageText("option 1"), MessageText("option 2"))
            val messageId = Messages.message(adminId, chatId, PollInput(MessageText("Title"), options))
            val expected = options.map { PollOption(it, votes = listOf()) }
            assertEquals(expected, PollMessages.read(messageId).options)
        }
    }
}
