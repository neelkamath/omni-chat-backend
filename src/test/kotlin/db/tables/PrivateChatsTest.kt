package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.ChatEdges
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.graphql.routing.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class PrivateChatsTest {
    @Nested
    inner class Create {
        @Test
        fun `Creating an existing chat must throw an exception`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val create = { PrivateChats.create(user1Id, user2Id) }
            create()
            assertFailsWith<IllegalArgumentException> { create() }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting the chat must wipe it from the DB`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            TypingStatuses.set(chatId, user1Id, isTyping = true)
            val messageId = Messages.message(user2Id, chatId)
            MessageStatuses.create(user1Id, messageId, MessageStatus.READ)
            Stargazers.create(user1Id, messageId)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChats.delete(chatId)
            listOf(Chats, Messages, MessageStatuses, PrivateChatDeletions, PrivateChats, Stargazers, TypingStatuses)
                .forEach { assertEquals(0, it.count()) }
        }
    }

    @Nested
    inner class Read {
        @Test
        fun `Reading a chat must give the ID of the user being chatted with, and not the user's own ID`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            val test = { userId: Int, otherUserId: Int ->
                assertEquals(otherUserId, PrivateChats.readUserChats(userId).first().user.id)
            }
            test(user1Id, user2Id)
            test(user2Id, user1Id)
        }

        @Test
        fun `Chats which did not have any activity after their deletion mustn't be read`() {
            val (user1Id, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
            val chat1Id = PrivateChats.create(user1Id, user2Id)
            val chat2Id = PrivateChats.create(user1Id, user3Id)
            val chat3Id = PrivateChats.create(user1Id, user4Id)
            PrivateChatDeletions.create(chat2Id, user1Id)
            Messages.create(user1Id, chat2Id)
            PrivateChatDeletions.create(chat3Id, user1Id)
            assertEquals(listOf(chat1Id, chat2Id), PrivateChats.readUserChats(user1Id).map { it.id })
        }
    }

    @Nested
    inner class Exists {
        @Test
        fun `A chat between two users must be said to exist`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            assertTrue(PrivateChats.exists(user1Id, user2Id))
            assertTrue(PrivateChats.exists(user2Id, user1Id))
        }

        @Test
        fun `If two users aren't in a chat with each other, it mustn't be said that they are`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.create(user2Id, user3Id)
            assertFalse(PrivateChats.exists(user1Id, user3Id))
            assertFalse(PrivateChats.exists(user3Id, user1Id))
        }
    }

    @Nested
    inner class QueryUserChatEdges {
        @Test
        fun `Chats must be queried`() {
            val (user1Id, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.info.id }
            val (chat1Id, chat2Id, chat3Id) = listOf(user2Id, user3Id, user4Id).map { PrivateChats.create(user1Id, it) }
            val queryText = "hi"
            val (message1, message2) = listOf(chat1Id, chat2Id).map {
                val messageId = Messages.message(user1Id, it, MessageText(queryText))
                MessageEdge(Messages.readMessage(user1Id, messageId), cursor = messageId)
            }
            Messages.create(user1Id, chat3Id, MessageText("bye"))
            val chat1Edges = ChatEdges(chat1Id, setOf(message1) as LinkedHashSet)
            val chat2Edges = ChatEdges(chat2Id, setOf(message2) as LinkedHashSet)
            assertEquals(setOf(chat1Edges, chat2Edges), PrivateChats.queryUserChatEdges(user1Id, queryText))
        }
    }

    @Nested
    inner class Search {
        @Test
        fun `Chats must be searched by case-insensitively querying usernames, email addresses, and names`() {
            val userId = createVerifiedUsers(1).first().info.id
            val userIdList = listOf(
                AccountInput(Username("dave_tompson"), Password("p"), emailAddress = "dave@example.com"),
                AccountInput(Username("iron_man_fan"), Password("p"), emailAddress = "tom@example.com"),
                AccountInput(
                    Username("vader"),
                    Password("p"),
                    emailAddress = "vader@example.com",
                    firstName = Name("Tommy")
                ),
                AccountInput(
                    Username("leia"),
                    Password("p"),
                    emailAddress = "leia@example.com",
                    lastName = Name("Tomas")
                ),
                AccountInput(Username("steve_rogers"), Password("p"), emailAddress = "steve@example.com")
            ).map {
                Users.create(it)
                val otherUserId = Users.read(it.username).id
                PrivateChats.create(userId, otherUserId)
                otherUserId
            }
            assertEquals(userIdList.dropLast(1), PrivateChats.search(userId, "tom").map { it.user.id })
        }
    }

    @Nested
    inner class ReadChatId {
        @Test
        fun `The chat's ID must be read if the participant is in the chat but the user isn't`() {
            val (participantId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(participantId, userId)
            PrivateChatDeletions.create(chatId, userId)
            assertEquals(chatId, PrivateChats.readChatId(participantId, userId))
        }

        @Test
        fun `Reading the ID of a chat the participant isn't in but the user is must fail`() {
            val (participantId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(participantId, userId)
            PrivateChatDeletions.create(chatId, participantId)
            assertFails { PrivateChats.readChatId(participantId, userId) }
        }
    }

    @Nested
    inner class ReadUserIdList {
        @Test
        fun `Retrieving the user IDs of a chat must return them`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertEquals(setOf(user1Id, user2Id), PrivateChats.readUserIdList(chatId))
        }
    }

    @Nested
    inner class ReadOtherUserId {
        @Test
        fun `The other user ID must be returned`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertEquals(user2Id, PrivateChats.readOtherUserId(chatId, user1Id))
            assertEquals(user1Id, PrivateChats.readOtherUserId(chatId, user2Id))
        }
    }

    @Nested
    inner class ReadOtherUserIdList {
        @Test
        fun `Reading the user ID list the user is chatting with must excluding those from deleted chats`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            PrivateChats.create(user1Id, user2Id)
            val chatId = PrivateChats.create(user1Id, user3Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(setOf(user2Id), PrivateChats.readOtherUserIdList(user1Id))
        }
    }
}
