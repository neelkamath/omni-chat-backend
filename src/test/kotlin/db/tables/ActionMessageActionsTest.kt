package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.graphql.routing.ActionMessageInput
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.toLinkedHashSet
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
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val actions = listOf(MessageText("action 1"), MessageText("action 2"))
            val messageId = Messages.message(adminId, chatId, ActionMessageInput(MessageText("text"), actions))
            assertEquals(actions.toLinkedHashSet(), ActionMessageActions.read(messageId))
        }
    }
}
