package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.component1
import com.neelkamath.omniChatBackend.component2
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.UserId
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.TriggeredAction
import com.neelkamath.omniChatBackend.graphql.routing.ActionMessageInput
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class ActionMessagesTest {
    @Nested
    inner class IsExisting {
        @Test
        fun `The message must be said to exist`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            val message = ActionMessageInput(MessageText("text"), listOf(MessageText("action")))
            val messageId = Messages.message(adminId, chatId, message)
            assertTrue(ActionMessages.isExisting(messageId))
        }

        @Test
        fun `The message mustn't be said to exist`() = assertFalse(ActionMessages.isExisting(messageId = 1))
    }

    @Nested
    inner class HasAction {
        @Test
        fun `Only the existing action must be said to exist`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            assertTrue(ActionMessages.hasAction(messageId, action))
            assertFalse(ActionMessages.hasAction(messageId, MessageText("non-existing action")))
        }
    }

    @Nested
    inner class IsValidTrigger {
        @Test
        fun `The trigger must be said to be valid`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            assertTrue(ActionMessages.isValidTrigger(adminId, messageId, action))
        }

        @Test
        fun `The trigger must be said to be invalid when the message is from a public chat the user isn't in`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val action = MessageText("Yes")
            val messageId = Messages.message(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            assertFalse(ActionMessages.isValidTrigger(userId, messageId, action))
        }
    }

    @Nested
    inner class Trigger {
        @Test
        fun `Only the message's creator must be notified of the triggered action`(): Unit = runBlocking {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.userId), listOf(user.userId))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                admin.userId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            awaitBrokering()
            val (adminSubscriber, userSubscriber) = listOf(admin.userId, user.userId)
                .map { messagesNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            ActionMessages.trigger(user.userId, messageId, action)
            awaitBrokering()
            val values = adminSubscriber.values().map { it as TriggeredAction }
            assertEquals(listOf(messageId), values.map { it.getMessageId() })
            assertEquals(listOf(action), values.map { it.getAction() })
            assertEquals(listOf(user.userId), values.map { it.getTriggeredBy().id })
            userSubscriber.assertNoValues()
        }
    }
}
