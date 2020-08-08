package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.MessagesAsset
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.graphql.routing.MessageText
import com.neelkamath.omniChat.graphql.routing.PollInput
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class PollMessagesTest : FunSpec({
    context("setVote(Int, Int, MessageText, Boolean)") {
        test("Setting a vote should only notify users in the chat") {
            val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val option1 = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(option1, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                .map { messagesNotifier.safelySubscribe(MessagesAsset(it)).subscribeWith(TestSubscriber()) }
            PollMessages.setVote(adminId, messageId, option1, vote = true)
            awaitBrokering()
            adminSubscriber.assertValue(Messages.readMessage(adminId, messageId).toUpdatedPollMessage())
            nonParticipantSubscriber.assertNoValues()
        }
    }

    context("hasOption(Int, MessageText)") {
        fun assertOptionExistence(exists: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val existentOption = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(existentOption, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val option = if (exists) existentOption else MessageText("nonexistent option")
            PollMessages.hasOption(messageId, option) shouldBe exists
        }

        test("The option should be said to exist") { assertOptionExistence(exists = true) }

        test("The option shouldn't be said to exist") { assertOptionExistence(exists = false) }
    }
})