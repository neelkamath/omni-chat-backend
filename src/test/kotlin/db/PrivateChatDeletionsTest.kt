package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.createVerifiedUsers
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe

class PrivateChatDeletionsTest : FunSpec({
    context("isDeleted(String, Int)") {
        test("The chat shouldn't be deleted if the user never deleted it") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.isDeleted(user1Id, chatId).shouldBeFalse()
        }

        test("The chat should be deleted if the user deleted it, and the other user didn't send a message after that") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChatDeletions.isDeleted(user1Id, chatId).shouldBeTrue()
        }

        test("The chat shouldn't be deleted if the other user sent a message after the user deleted it") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(chatId, user2Id, "text")
            PrivateChatDeletions.isDeleted(user1Id, chatId).shouldBeFalse()
        }

        test("The chat shouldn't be deleted if the user sent a message to the other user after deleting their chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(chatId, user1Id, "text")
            PrivateChatDeletions.isDeleted(user1Id, chatId).shouldBeFalse()
        }

        test(
            """
            Given a chat deleted by the user which had no activity after its deletion,
            when checking if the chat is deleted for the other user,
            then it should be false
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user2Id)
            PrivateChatDeletions.isDeleted(user1Id, chatId).shouldBeFalse()
        }
    }

    context("create(Int, String)") {
        test("Deleting a chat the user was never in should fail") {
            val userId = createVerifiedUsers(1)[0].info.id
            shouldThrowExactly<IllegalArgumentException> { PrivateChatDeletions.create(chatId = 1, userId = userId) }
        }

        test("Deleting a chat the user was in but just deleted shouldn't fail") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            repeat(2) { PrivateChatDeletions.create(chatId, user1Id) }
        }
    }

    context("readLastDeletion(Int)") {
        test("Only messages deleted by both users should be deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(chatId, user1Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            val message1Id = Messages.message(chatId, user2Id, "text")
            PrivateChatDeletions.create(chatId, user2Id)
            val message2Id = Messages.message(chatId, user1Id, "text")
            Messages.readPrivateChat(chatId, user1Id).map { it.cursor } shouldBe listOf(message1Id, message2Id)
        }
    }

    context("deletePreviousDeletionRecords(Int, String)") {
        test("A user must have at most one record of deleting a chat") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            repeat(3) { PrivateChatDeletions.create(chatId, user1Id) }
            PrivateChatDeletions.count() shouldBe 1
        }
    }

    context("deleteUnusedChatData(Int, String)") {
        test("Messages deleted by one user shouldn't be deleted for the other user") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(chatId, user1Id, "text")
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.readPrivateChat(chatId, user2Id).shouldNotBeEmpty()
        }

        test(
            """
            Given a chat deleted by the user which had no activity after its deletion,
            when the other user deletes the chat,
            then the chat's records should be deleted
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            PrivateChatDeletions.create(chatId, user2Id)
            PrivateChats.count().shouldBeZero()
            PrivateChatDeletions.count().shouldBeZero()
        }

        test(
            """
            Given a chat deleted by the user which had activity after its deletion,
            when the other user deletes the chat,
            then the chat shouldn't be deleted
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            Messages.create(chatId, user2Id, "text")
            PrivateChatDeletions.create(chatId, user2Id)
            PrivateChats.count() shouldBe 1
        }
    }
})