package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createUser
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ChatEdges
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.readUserByUsername
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

    context("delete(Int)") {
        test("Deleting the chat should wipe it from the DB") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            TypingStatuses.set(chatId, user1Id, isTyping = true)
            val messageId = Messages.message(user2Id, chatId)
            MessageStatuses.create(user1Id, messageId, MessageStatus.READ)
            Stargazers.create(user1Id, messageId)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChats.delete(chatId)
            listOf(Chats, Messages, MessageStatuses, PrivateChatDeletions, PrivateChats, Stargazers, TypingStatuses)
                .forEach { it.count().shouldBeZero() }
        }
    }

    context("read(String, BackwardPagination?)") {
        test("Reading a chat should give the ID of the user being chatted with, and not the user's own ID") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            val test = { userId: Int, otherUserId: Int ->
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
            Messages.create(user1Id, chat2Id)
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
            val (chat1Id, chat2Id, chat3Id) = listOf(user2Id, user3Id, user4Id).map { PrivateChats.create(user1Id, it) }
            val queryText = "hi"
            val (message1, message2) = listOf(chat1Id, chat2Id).map {
                val messageId = Messages.message(user1Id, it, MessageText(queryText))
                MessageEdge(Messages.readMessage(user1Id, messageId), cursor = messageId)
            }
            Messages.create(user1Id, chat3Id, MessageText("bye"))
            val chat1Edges = ChatEdges(chat1Id, listOf(message1))
            val chat2Edges = ChatEdges(chat2Id, listOf(message2))
            PrivateChats.queryUserChatEdges(user1Id, queryText) shouldBe listOf(chat1Edges, chat2Edges)
        }
    }

    context("search(String, String, BackwardPagination?)") {
        test(
            """
            Chats should be searched by case-insensitively querying usernames, email addresses, first names, and 
            last names
            """
        ) {
            val userId = createVerifiedUsers(1)[0].info.id
            val userIdList = listOf(
                AccountInput(Username("dave_tompson"), Password("p"), emailAddress = "dave@example.com"),
                AccountInput(Username("iron man fan"), Password("p"), emailAddress = "tom@example.com"),
                AccountInput(Username("vader"), Password("p"), emailAddress = "vader@example.com", firstName = "Tommy"),
                AccountInput(Username("leia"), Password("p"), emailAddress = "leia@example.com", lastName = "Tomas"),
                AccountInput(Username("steve_rogers"), Password("p"), emailAddress = "steve@example.com")
            ).map {
                createUser(it)
                val otherUserId = readUserByUsername(it.username).id
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

    context("readUserIdList(Int)") {
        test("Retrieving the user IDs of a chat should return them") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChats.readUserIdList(chatId) shouldBe listOf(user1Id, user2Id)
        }
    }

    context("readOtherUserId(Int, String)") {
        test("The other user ID should be returned") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChats.readOtherUserId(chatId, user1Id) shouldBe user2Id
            PrivateChats.readOtherUserId(chatId, user2Id) shouldBe user1Id
        }
    }

    context("readOtherUserIdList(String)") {
        test("Reading the user ID list the user is chatting with should include those from deleted chats") {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = listOf(user2Id, user3Id).map { PrivateChats.create(user1Id, it) }[0]
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChats.readOtherUserIdList(user1Id) shouldBe listOf(user2Id, user3Id)
        }
    }
})
