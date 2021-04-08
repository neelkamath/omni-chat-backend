package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.routing.ActionMessageInput
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.graphql.routing.TriggeredAction
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class ActionMessagesTest {
    @Nested
    inner class HasAction {
        @Test
        fun `Only the existent action must be said to exist`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            assertTrue(ActionMessages.hasAction(messageId, action))
            assertFalse(ActionMessages.hasAction(messageId, MessageText("nonexistent action")))
        }
    }

    @Nested
    inner class Trigger {
        @Test
        fun `Only the message's creator must be notified of the triggered action`(): Unit = runBlocking {
            val (admin, user) = createVerifiedUsers(2).map { it.info }
            val chatId = GroupChats.create(listOf(admin.id), listOf(user.id))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                admin.id,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            awaitBrokering()
            val (adminSubscriber, userSubscriber) =
                listOf(admin.id, user.id).map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
            ActionMessages.trigger(user.id, messageId, action)
            awaitBrokering()
            adminSubscriber.assertValue(TriggeredAction(messageId, action, user))
            userSubscriber.assertNoValues()
        }
    }
}
