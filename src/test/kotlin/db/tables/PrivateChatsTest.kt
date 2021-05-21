package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.DbExtension
import com.neelkamath.omniChatBackend.createVerifiedUsers
import com.neelkamath.omniChatBackend.db.ChatEdges
import com.neelkamath.omniChatBackend.db.count
import com.neelkamath.omniChatBackend.graphql.routing.*
import com.neelkamath.omniChatBackend.linkedHashSetOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class PrivateChatsTest {
    @Nested
    inner class Create {
        @Test
        fun `Creating an existing chat must throw an exception`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val create = { PrivateChats.create(user1Id, user2Id) }
            create()
            assertFailsWith<IllegalArgumentException> { create() }
        }
    }

    @Nested
    inner class ReadChatId {
        @Test
        fun `The chat's ID must be read`() {
            val (participantId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(participantId, userId)
            PrivateChatDeletions.create(chatId, userId)
            assertEquals(chatId, PrivateChats.readChatId(participantId, userId))
        }
    }

    @Nested
    inner class QueryUserChatEdges {
        @Test
        fun `Chats must be queried`() {
            val (user1Id, user2Id, user3Id, user4Id) = createVerifiedUsers(4).map { it.userId }
            val (chat1Id, chat2Id, chat3Id) = listOf(user2Id, user3Id, user4Id).map { PrivateChats.create(user1Id, it) }
            val queryText = "hi"
            val (message1, message2) =
                listOf(chat1Id, chat2Id).map { Messages.message(user1Id, it, MessageText(queryText)) }
            Messages.message(user1Id, chat3Id, MessageText("bye"))
            val chat1Edges = ChatEdges(chat1Id, linkedHashSetOf(message1))
            val chat2Edges = ChatEdges(chat2Id, linkedHashSetOf(message2))
            assertEquals(setOf(chat1Edges, chat2Edges), PrivateChats.queryUserChatEdges(user1Id, queryText))
        }
    }

    @Nested
    inner class IsExisting {
        @Test
        fun `A chat between two users must be said to exist`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            PrivateChats.create(user1Id, user2Id)
            assertTrue(PrivateChats.isExisting(user1Id, user2Id))
            assertTrue(PrivateChats.isExisting(user2Id, user1Id))
        }

        @Test
        fun `If two users aren't in a chat with each other, it mustn't be said that they are`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
            PrivateChats.create(user1Id, user2Id)
            PrivateChats.create(user2Id, user3Id)
            assertFalse(PrivateChats.isExisting(user1Id, user3Id))
            assertFalse(PrivateChats.isExisting(user3Id, user1Id))
        }
    }

    @Nested
    inner class Search {
        @Test
        fun `Chats must be searched by case-insensitively querying usernames, email addresses, and names`() {
            val userId = createVerifiedUsers(1).first().userId
            val userIdList = listOf(
                AccountInput(Username("dave_tompson"), Password("p"), emailAddress = "dave@example.com"),
                AccountInput(Username("iron_man_fan"), Password("p"), emailAddress = "tom@example.com"),
                AccountInput(
                    Username("vader"),
                    Password("p"),
                    emailAddress = "vader@example.com",
                    firstName = Name("Tommy"),
                ),
                AccountInput(
                    Username("leia"),
                    Password("p"),
                    emailAddress = "leia@example.com",
                    lastName = Name("Tomas"),
                ),
                AccountInput(Username("steve_rogers"), Password("p"), emailAddress = "steve@example.com"),
            ).map {
                Users.create(it)
                val otherUserId = Users.search(it.username.value).first()
                PrivateChats.create(userId, otherUserId)
                otherUserId
            }
            val actual = PrivateChats.search(userId, "tom").map { PrivateChats.readOtherUserId(it, userId) }
            assertEquals(userIdList.dropLast(1), actual)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting the chat must wipe it from the DB`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            TypingStatuses.update(chatId, user1Id, isTyping = true)
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
    inner class ReadUserIdList {
        @Test
        fun `Retrieving the user IDs of a chat must return them`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertEquals(setOf(user1Id, user2Id), PrivateChats.readUserIdList(chatId))
        }
    }

    @Nested
    inner class ReadOtherUserId {
        @Test
        fun `The other user ID must be returned`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertEquals(user2Id, PrivateChats.readOtherUserId(chatId, user1Id))
            assertEquals(user1Id, PrivateChats.readOtherUserId(chatId, user2Id))
        }
    }
}
