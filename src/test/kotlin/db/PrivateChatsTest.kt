package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.db.PrivateChats
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class PrivateChatsTest : StringSpec({
    listener(DbListener())

    "A chat should be said to exist between two users" {
        val creator = "creator user ID"
        val invitee = "invitee user ID"
        PrivateChats.create(creator, invitee)
        PrivateChats.exists(creator, invitee).shouldBeTrue()
        PrivateChats.exists(invitee, creator).shouldBeTrue()
    }

    "A chat should be said to not exist between two users" {
        val users = (1..3).map { "user ID $it" }
        PrivateChats.create(users[0], users[1])
        PrivateChats.create(users[1], users[2])
        PrivateChats.exists(users[0], users[2]).shouldBeFalse()
        PrivateChats.exists(users[2], users[0]).shouldBeFalse()
    }
})