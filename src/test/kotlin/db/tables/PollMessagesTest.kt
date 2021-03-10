package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.PollInput
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
                val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId))
                val option1 = MessageText("option 1")
                val poll = PollInput(MessageText("Title"), listOf(option1, MessageText("option 2")))
                val messageId = Messages.message(adminId, chatId, poll)
                awaitBrokering()
                val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                    .map { messagesNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                PollMessages.setVote(adminId, messageId, option1, vote = true)
                awaitBrokering()
                adminSubscriber.assertValue(Messages.readMessage(adminId, messageId).toUpdatedPollMessage())
                nonParticipantSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class HasOption {
        private fun assertOptionExistence(exists: Boolean) {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val existentOption = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(existentOption, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val option = if (exists) existentOption else MessageText("nonexistent option")
            assertEquals(exists, PollMessages.hasOption(messageId, option))
        }

        @Test
        fun `The option must be said to exist`() {
            assertOptionExistence(exists = true)
        }

        @Test
        fun `The option mustn't be said to exist`() {
            assertOptionExistence(exists = false)
        }
    }
}
