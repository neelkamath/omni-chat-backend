package com.neelkamath.omniChat.test.db

import com.neelkamath.omniChat.NewGroupChat
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.test.createVerifiedUsers
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe

class DbTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    context("deleteUserFromDb(String)") {
        test("An exception should be thrown when the admin of a nonempty group chat deletes their data") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            GroupChats.create(adminId, chat)
            shouldThrowExactly<IllegalArgumentException> { deleteUserFromDb(adminId) }
        }

        test(
            "A private chat deleted by the user should be deleted for the other user when the user deletes their data"
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            deleteUserFromDb(user1Id)
            PrivateChats.count().shouldBeZero()
        }
    }

    context("readChat(Int, String, BackwardPagination?)") {
        test("A private chat should be read") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            readChat(chatId, user1Id).id shouldBe chatId
        }

        test("A group chat should be read") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            readChat(chatId, adminId).id shouldBe chatId
        }

        test("Reading a chat the user isn't in should throw an exception") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            shouldThrowExactly<IllegalArgumentException> { readChat(chatId, userId) }
        }

        test("A private chat the user deleted should be read") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            readChat(chatId, user1Id)
        }
    }
}