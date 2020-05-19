package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.MessageStatus
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.PrivateChat
import com.neelkamath.omniChat.db.MessageStatuses
import com.neelkamath.omniChat.db.Messages
import com.neelkamath.omniChat.db.PrivateChatDeletions
import com.neelkamath.omniChat.db.PrivateChats
import com.neelkamath.omniChat.findUserByUsername
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.createVerifiedUsers
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.mutations.createPrivateChat
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe

class PrivateChatsTest : FunSpec({
    listener(AppListener())

    context("create(String, String)") {
        test("Creating an existing chat should throw an exception") {
            val create = { PrivateChats.create("user 1 ID", "user 2 ID") }
            create()
            shouldThrowExactly<IllegalArgumentException> { create() }
        }
    }

    context("read(String)") {
        test("Reading a chat should give the ID of the user being chatted with, and not the user's own ID") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val test = { userId: String, otherUserId: String ->
                val chat = PrivateChat(chatId, otherUserId, messages = listOf())
                PrivateChats.read(userId) shouldBe listOf(chat)
            }
            test(user1Id, user2Id)
            test(user2Id, user1Id)
        }
    }

    context("exists(String, String)") {
        test("A chat between two users should be said to exist") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.exists(user1Id, user2Id).shouldBeTrue()
            PrivateChats.exists(user2Id, user1Id).shouldBeTrue()
        }

        test("If two users aren't in a chat with each other, it shouldn't be said that they are") {
            val (user1Id, user2Id, user3Id) = (1..3).map { "user $it ID" }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.create(user2Id, user3Id)
            PrivateChats.exists(user1Id, user3Id).shouldBeFalse()
            PrivateChats.exists(user3Id, user1Id).shouldBeFalse()
        }
    }

    context("search(String, String)") {
        test(
            """
            Chats should be searched by case-insensitively querying usernames, email addresses, first names, and last 
            names
            """
        ) {
            val user = createVerifiedUsers(1)[0]
            val userIdList = listOf(
                NewAccount(username = "dave_tompson", password = "p", emailAddress = "dave@example.com"),
                NewAccount(username = "iron man fan", password = "p", emailAddress = "tom@example.com"),
                NewAccount(username = "vader", password = "p", emailAddress = "vader@example.com", firstName = "Tommy"),
                NewAccount(username = "leia", password = "p", emailAddress = "leia@example.com", lastName = "Tomas"),
                NewAccount(username = "steve_rogers", password = "p", emailAddress = "steve@example.com")
            ).map {
                createAccount(it)
                val userId = findUserByUsername(it.username).id
                createPrivateChat(userId, user.accessToken)
                userId
            }
            PrivateChats.search(user.info.id, "tom").map { it.userId } shouldBe userIdList.dropLast(1)
        }
    }

    context("delete(Int)") {
        /**
         * Creates a private chat between [user1Id] and [user2Id], has [user1Id] send a message in it, has [user2Id]
         * create [MessageStatuses] for the sent message, and returns the chat's ID.
         */
        fun createAndUseChat(user1Id: String, user2Id: String): Int {
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user1Id, "text")
            MessageStatuses.create(messageId, user2Id, MessageStatus.READ)
            return chatId
        }

        test(
            """
            Given a chat which hasn't been deleted, and a chat deleted by the user which had no activity after its 
            deletion, 
            
            when the user's chats are deleted,
            
            then both the chats (along with their chat deletion records, messages, and message statuses) should be 
            deleted
            """
        ) {
            val (user1Id, user2Id, user3Id) = (1..3).map { "user $it ID" }
            createAndUseChat(user1Id, user2Id)
            val chatId = createAndUseChat(user1Id, user3Id)
            PrivateChatDeletions.delete(chatId)
            PrivateChats.delete(user1Id)
            PrivateChats.count().shouldBeZero()
            PrivateChatDeletions.count().shouldBeZero()
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }
    }

    context("readUsers(Int)") {
        test("Retrieving the user IDs of a chat should return them") {
            val (user1Id, user2Id) = (1..2).map { "user $it ID" }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChats.readUsers(chatId) shouldBe listOf(user1Id, user2Id)
        }
    }
})