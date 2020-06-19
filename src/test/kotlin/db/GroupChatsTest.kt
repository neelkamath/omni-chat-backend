package com.neelkamath.omniChat.db

import com.neelkamath.omniChat.*
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe

class GroupChatsTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    context("create(String, NewGroupChat") {
        test("A chat should be created which includes the admin in the list of users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            GroupChats.readChat(chatId) shouldBe GroupChat(
                chatId,
                adminId,
                (chat.userIdList + adminId).map(::findUserById),
                chat.title,
                chat.description,
                emptyMessagesConnection
            )
        }
    }

    context("setAdmin(Int, String)") {
        test("The new admin should be set") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            GroupChats.setAdmin(chatId, userId)
            GroupChats.isAdmin(userId, chatId).shouldBeTrue()
        }

        test("Setting the admin to a user who isn't in the chat should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            shouldThrowExactly<IllegalArgumentException> { GroupChats.setAdmin(chatId, "new admin ID") }
        }
    }

    context("update(GroupChatUpdate)") {
        test("Updating the chat to have an empty title should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val update = GroupChatUpdate(chatId, title = "   ")
            shouldThrowExactly<IllegalArgumentException> { GroupChats.update(update) }
        }

        test("When a user leaves a group chat, their subscription to it should be removed") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val subscriber = createMessageUpdatesSubscriber(adminId, chatId)
            val update = GroupChatUpdate(chatId, removedUserIdList = listOf(adminId))
            GroupChats.update(update)
            subscriber.assertComplete()
        }

        test("The chat should be deleted once every user has left it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            val chat = NewGroupChat("Title", userIdList = userIdList)
            val chatId = GroupChats.create(adminId, chat)
            val update = GroupChatUpdate(chatId, removedUserIdList = userIdList + adminId)
            GroupChats.update(update)
            GroupChats.count().shouldBeZero()
        }
    }

    context("removeUsers(Int, List<String>)") {
        test("Subscriptions should be removed for deleted chats") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            val subscriber = createMessageUpdatesSubscriber(adminId, chatId)
            GroupChatUsers.removeUsers(chatId, listOf(adminId))
            subscriber.assertComplete()
        }

        test("Messages from a user who left should be retained") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val messageId = Messages.message(chatId, userId, "text")
            GroupChatUsers.removeUsers(chatId, listOf(userId))
            Messages.readIdList(chatId) shouldBe listOf(messageId)
        }

        test("When the user leaves the chat they were accessing on two devices, both subscriptions should be removed") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val phoneSubscriber = createMessageUpdatesSubscriber(userId, chatId)
            val laptopSubscriber = createMessageUpdatesSubscriber(userId, chatId)
            GroupChatUsers.removeUsers(chatId, listOf(userId))
            phoneSubscriber.assertComplete()
            laptopSubscriber.assertComplete()
        }
    }

    context("delete(Int)") {
        test("Deleting a nonempty chat should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, NewGroupChat("Title"))
            shouldThrowExactly<IllegalArgumentException> { GroupChats.delete(chatId) }
        }

        test("Deleting a chat should delete it along with its messages and messages statuses") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val messageId = Messages.message(chatId, adminId, "text")
            MessageStatuses.create(messageId, userId, MessageStatus.READ)
            GroupChatUsers.removeUsers(chatId, listOf(adminId, userId)) // The chat is deleted once every user has left.
            GroupChats.count().shouldBeZero()
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }
    }

    context("isNonemptyChatAdmin(String)") {
        test("A non-admin shouldn't be the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            GroupChats.create(adminId, chat)
            GroupChats.isNonemptyChatAdmin(userId).shouldBeFalse()
        }

        test("An admin of an empty group chat shouldn't be the admin of a nonempty group chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            GroupChats.create(adminId, NewGroupChat("Title"))
            GroupChats.isNonemptyChatAdmin(adminId).shouldBeFalse()
        }

        test("An admin of a nonempty group chat should be the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat("Title", userIdList = listOf(userId))
            GroupChats.create(adminId, chat)
            GroupChats.isNonemptyChatAdmin(adminId).shouldBeTrue()
        }
    }

    context("queryIdList(String, String)") {
        test("Chats should be queried") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id, chat3Id) = (1..3).map { GroupChats.create(adminId, NewGroupChat("Title")) }
            val queryText = "hi"
            listOf(chat1Id, chat2Id).map { Messages.create(it, adminId, queryText) }
            Messages.create(chat3Id, adminId, "bye")
            GroupChats.queryIdList(adminId, queryText) shouldBe listOf(chat1Id, chat2Id)
        }
    }

    context("search(String, String, BackwardPagination?)") {
        test("Chats should be searched case-insensitively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chats = listOf(NewGroupChat("Title 1"), NewGroupChat("Title 2"), NewGroupChat("Iron Man Fan Club"))
            chats.forEach { GroupChats.create(adminId, it) }
            GroupChats.search(adminId, "itle ").map { it.title } shouldBe listOf(chats[0].title, chats[1].title)
            GroupChats.search(adminId, "iron").map { it.title } shouldBe listOf(chats[2].title)
        }
    }
}
