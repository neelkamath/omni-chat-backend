package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.awaitBrokering
import com.neelkamath.omniChatBackend.db.messagesNotifier
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedMessage
import com.neelkamath.omniChatBackend.graphql.routing.MessageText
import com.neelkamath.omniChatBackend.graphql.routing.PollInput
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(DbExtension::class)
class PollMessagesTest {
    @Nested
    inner class SetVote {
        @Test
        fun `Setting a vote must only notify users in the chat`() {
            runBlocking {
                val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.userId }
                val chatId = GroupChats.create(listOf(adminId))
                val option1 = MessageText("option 1")
                val poll = PollInput(MessageText("Title"), listOf(option1, MessageText("option 2")))
                val messageId = Messages.message(adminId, chatId, poll)
                awaitBrokering()
                val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                    .map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                PollMessages.setVote(adminId, messageId, option1, vote = true)
                awaitBrokering()
                val actual = adminSubscriber.values().map { (it as UpdatedMessage).getMessageId() }
                assertEquals(listOf(messageId), actual)
                nonParticipantSubscriber.assertNoValues()
            }
        }
    }
}
