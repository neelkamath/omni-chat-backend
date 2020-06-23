package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe

class PrivateChatsTest : FunSpec({
    context("create(String, String)") {
        test("Creating an existing chat should throw an exception") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val create = { PrivateChats.create(user1Id, user2Id) }
            create()
            shouldThrowExactly<IllegalArgumentException> { create() }
        }
    }

    context("read(String, BackwardPagination?)") {
        test("Reading a chat should give the ID of the user being chatted with, and not the user's own ID") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            val test = { userId: String, otherUserId: String ->
                PrivateChats.readUserChats(userId)[0].user.id shouldBe otherUserId
            }
            test(user1Id, user2Id)
            test(user2Id, user1Id)
        }

        test("Chats which didn't have any activity after their deletion shouldn't be read") {
            val (user1Id, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
            val chat1Id = PrivateChats.create(user1Id, user2Id)
            val chat2Id = PrivateChats.create(user1Id, user3Id)
            val chat3Id = PrivateChats.create(user1Id, user4Id)
            PrivateChatDeletions.create(chat2Id, user1Id)
            Messages.create(chat2Id, user1Id, "text")
            PrivateChatDeletions.create(chat3Id, user1Id)
            PrivateChats.readUserChats(user1Id).map { it.id } shouldBe listOf(chat1Id, chat2Id)
        }
    }

    context("exists(String, String)") {
        test("A chat between two users should be said to exist") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.exists(user1Id, user2Id).shouldBeTrue()
            PrivateChats.exists(user2Id, user1Id).shouldBeTrue()
        }

        test("If two users aren't in a chat with each other, it shouldn't be said that they are") {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.create(user2Id, user3Id)
            PrivateChats.exists(user1Id, user3Id).shouldBeFalse()
            PrivateChats.exists(user3Id, user1Id).shouldBeFalse()
        }
    }

    context("queryUserChatEdges(String, String)") {
        test("Chats should be queried") {
            val (user1Id, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
            val queryText = "hi"
            val (chat1Id, chat2Id) = listOf(user2Id, user3Id).map { PrivateChats.create(user1Id, it) }
            val (message1, message2) = listOf(chat1Id, chat2Id).map {
                val id = Messages.message(it, user1Id, queryText)
                MessageEdge(Messages.read(id), cursor = id)
            }
            val chat3Id = PrivateChats.create(user1Id, user4Id)
            Messages.create(chat3Id, user1Id, "bye")
            PrivateChats.queryUserChatEdges(user1Id, queryText) shouldBe
                    listOf(ChatEdges(chat1Id, listOf(message1)), ChatEdges(chat2Id, listOf(message2)))
        }
    }

    context("search(String, String, BackwardPagination?)") {
        test(
            """
            Chats should be searched by case-insensitively querying usernames, email addresses, first names, and last 
            names
            """
        ) {
            val userId = createVerifiedUsers(1)[0].info.id
            val userIdList = listOf(
                NewAccount(username = "dave_tompson", password = "p", emailAddress = "dave@example.com"),
                NewAccount(username = "iron man fan", password = "p", emailAddress = "tom@example.com"),
                NewAccount(username = "vader", password = "p", emailAddress = "vader@example.com", firstName = "Tommy"),
                NewAccount(username = "leia", password = "p", emailAddress = "leia@example.com", lastName = "Tomas"),
                NewAccount(username = "steve_rogers", password = "p", emailAddress = "steve@example.com")
            ).map {
                createUser(it)
                val otherUserId = findUserByUsername(it.username).id
                PrivateChats.create(userId, otherUserId)
                otherUserId
            }
            PrivateChats.search(userId, "tom").map { it.user.id } shouldBe userIdList.dropLast(1)
        }
    }

    context("areInChat(String, String)") {
        test("Two users should be said to be in a chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.areInChat(user1Id, user2Id).shouldBeTrue()
        }

        test("Two users shouldn't be said to be in a chat if one of them deleted the chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChats.areInChat(user1Id, user2Id).shouldBeFalse()
        }
    }

    context("readChatId(String, String)") {
        test("""The chat's ID should be read if the "participant" is in the chat but the "user" isn't""") {
            val (participantId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(participantId, userId)
            PrivateChatDeletions.create(chatId, userId)
            PrivateChats.readChatId(participantId, userId) shouldBe chatId
        }

        test("""Reading the ID of a chat the "participant" isn't in but the "user" is should fail""") {
            val (participantId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(participantId, userId)
            PrivateChatDeletions.create(chatId, participantId)
            shouldThrowAny { PrivateChats.readChatId(participantId, userId) }
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
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            createAndUseChat(user1Id, user2Id)
            val chatId = createAndUseChat(user1Id, user3Id)
            PrivateChatDeletions.delete(chatId)
            PrivateChats.deleteUserChats(user1Id)
            PrivateChats.count().shouldBeZero()
            PrivateChatDeletions.count().shouldBeZero()
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }
    }

    context("readUsers(Int)") {
        test("Retrieving the user IDs of a chat should return them") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChats.readUsers(chatId) shouldBe listOf(user1Id, user2Id)
        }
    }
})