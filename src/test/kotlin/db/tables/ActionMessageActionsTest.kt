package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.graphql.routing.ActionMessageInput
import com.neelkamath.omniChat.graphql.routing.MessageText
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class ActionMessageActionsTest {
    @Nested
    inner class Read {
        @Test
        fun `Actions must be read in the order they were created`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val actions = listOf(MessageText("action 1"), MessageText("action 2"))
            val messageId = Messages.message(adminId, chatId, ActionMessageInput(MessageText("text"), actions))
            assertEquals(actions, ActionMessages.read(messageId).actions)
        }
    }
}
