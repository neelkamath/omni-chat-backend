package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.operations.triggerAction
import com.neelkamath.omniChat.graphql.routing.ActionMessageInput
import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.TriggeredAction
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
        fun `Only the existent action should be said to exist`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                    adminId,
                    chatId,
                    ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No")))
            )
            assertTrue(ActionMessages.hasAction(messageId, action))
            assertFalse(ActionMessages.hasAction(messageId, MessageText("nonexistent action")))
        }
    }

    @Nested
    inner class Trigger {
        @Test
        fun `Only the message's creator should be notified of the triggered action`(): Unit = runBlocking {
            val (admin, user) = createVerifiedUsers(2).map { it.info }
            val chatId = GroupChats.create(listOf(admin.id), listOf(user.id))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                    admin.id,
                    chatId,
                    ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No")))
            )
            val (adminSubscriber, userSubscriber) = listOf(admin.id, user.id)
                    .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            triggerAction(user.id, messageId, action)
            awaitBrokering()
            adminSubscriber.assertValue(TriggeredAction(messageId, action, user))
            userSubscriber.assertNoValues()
        }
    }
}
