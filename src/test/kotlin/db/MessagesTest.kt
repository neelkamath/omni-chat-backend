package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.db.PrivateChats
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MessagesTest : StringSpec({
    listener(DbListener())

    data class CreatedMessage(val creator: String, val message: String)

    "Messages should be read in the order of their creation" {
        val creator = "creator user ID"
        val invitee = "invitee user ID"
        val chatId = PrivateChats.create(creator, invitee)
        val createdMessages = listOf(
            CreatedMessage(creator, "Hey"),
            CreatedMessage(invitee, "Hi!"),
            CreatedMessage(creator, "I have a question"),
            CreatedMessage(creator, "Is tomorrow a holiday?")
        )
        createdMessages.forEach { Messages.create(chatId, it.creator, it.message) }
        Messages.read(chatId).mapIndexed { index, privateMessage ->
            privateMessage.userId shouldBe createdMessages[index].creator
            privateMessage.message shouldBe createdMessages[index].message
        }
    }
})