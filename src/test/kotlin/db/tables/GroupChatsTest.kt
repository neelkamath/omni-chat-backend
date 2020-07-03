package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class GroupChatTitleTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the title is only whitespace") {
            shouldThrowExactly<IllegalArgumentException> { GroupChatTitle("  ") }
        }

        test("An exception should be thrown if the title is too long") {
            val title = CharArray(GroupChats.MAX_TITLE_LENGTH + 1) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { GroupChatTitle(title) }
        }

        test("An exception should be thrown if the title is too short") {
            shouldThrowExactly<IllegalArgumentException> { GroupChatTitle("") }
        }
    }
})

class GroupChatDescriptionTest : FunSpec({
    context("init") {
        test("An exception should be thrown if the description is too long") {
            val description = CharArray(GroupChats.MAX_DESCRIPTION_LENGTH + 1) { 'a' }.joinToString("")
            shouldThrowExactly<IllegalArgumentException> { GroupChatDescription(description) }
        }
    }
})

class GroupChatsTest : FunSpec({
    context("create(String, NewGroupChat") {
        test("A chat should be created which includes the admin in the list of users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            GroupChats.readChat(chatId).users.edges.map { it.node.id } shouldContainExactlyInAnyOrder
                    listOf(adminId, userId)
        }
    }

    context("setAdmin(Int, String)") {
        test("The new admin should be set") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            GroupChats.setAdmin(chatId, userId)
            GroupChats.isAdmin(userId, chatId).shouldBeTrue()
        }

        test("Setting the admin to a user who isn't in the chat should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            shouldThrowExactly<IllegalArgumentException> { GroupChats.setAdmin(chatId, "new admin ID") }
        }
    }

    context("update(UpdatedGroupChat)") {
        test("When a user leaves a group chat, their subscription to it should be removed") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val subscriber = messagesBroker
                .subscribe(MessagesAsset(adminId, chatId))
                .subscribeWith(TestSubscriber())
            val update = GroupChatUpdate(chatId, removedUserIdList = listOf(adminId))
            GroupChats.update(update)
            subscriber.assertComplete()
        }

        test("The chat should be deleted once every user has left it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            val chatId = GroupChats.create(adminId, userIdList)
            val update = GroupChatUpdate(chatId, removedUserIdList = userIdList + adminId)
            GroupChats.update(update)
            GroupChats.count().shouldBeZero()
        }

        test("A subscriber should be notified when the chat is updated") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId)
            val subscriber = groupChatInfoBroker
                .subscribe(GroupChatInfoAsset(chatId, adminId))
                .subscribeWith(TestSubscriber())
            val update = GroupChatUpdate(chatId, GroupChatTitle("New Title"), newUserIdList = listOf(userId))
            GroupChats.update(update)
            subscriber.assertValue(update.toUpdatedGroupChat())
        }
    }

    context("removeUsers(Int, List<String>)") {
        test("Subscriptions should be removed for deleted chats") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            val subscriber = messagesBroker
                .subscribe(MessagesAsset(adminId, chatId))
                .subscribeWith(TestSubscriber())
            GroupChatUsers.removeUsers(chatId, adminId)
            subscriber.assertComplete()
        }

        test("Messages from a user who left should be retained") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            val messageId = Messages.message(chatId, userId, TextMessage("text"))
            GroupChatUsers.removeUsers(chatId, userId)
            Messages.readIdList(chatId) shouldBe listOf(messageId)
        }

        test("When the user leaves the chat they were accessing on two devices, both subscriptions should be removed") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            val (phoneSubscriber, laptopSubscriber) = (1..2).map {
                messagesBroker
                    .subscribe(MessagesAsset(userId, chatId))
                    .subscribeWith(TestSubscriber())
            }
            GroupChatUsers.removeUsers(chatId, userId)
            phoneSubscriber.assertComplete()
            laptopSubscriber.assertComplete()
        }
    }

    context("delete(Int)") {
        test("Deleting a nonempty chat should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            shouldThrowExactly<IllegalArgumentException> { GroupChats.delete(chatId) }
        }

        test("Deleting a chat should delete it along with its messages and messages statuses") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, listOf(userId))
            val messageId = Messages.message(chatId, adminId, TextMessage("text"))
            MessageStatuses.create(messageId, userId, MessageStatus.READ)
            GroupChatUsers.removeUsers(chatId, adminId, userId)
            GroupChats.count().shouldBeZero()
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }

        test("Deleting a chat should unsubscribe subscribers only for that chat") {
            val (admin1Id, userId, admin2Id) = createVerifiedUsers(3).map { it.info.id }
            val chat1Id = GroupChats.create(admin1Id, listOf(userId))
            val chat2Id = GroupChats.create(admin2Id)
            val (admin1Subscriber, userSubscriber, admin2Subscriber) =
                mapOf(admin1Id to chat1Id, userId to chat1Id, admin2Id to chat2Id).map { (userId, chatId) ->
                    messagesBroker
                        .subscribe(MessagesAsset(userId, chatId))
                        .subscribeWith(TestSubscriber())
                }
            GroupChatUsers.removeUsers(chat1Id, admin1Id, userId)
            admin1Subscriber.assertComplete()
            userSubscriber.assertComplete()
            admin2Subscriber.assertNotComplete()
        }
    }

    context("isNonemptyChatAdmin(String)") {
        test("A non-admin shouldn't be the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, listOf(userId))
            GroupChats.isNonemptyChatAdmin(userId).shouldBeFalse()
        }

        test("An admin of an empty group chat shouldn't be the admin of a nonempty group chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            GroupChats.create(adminId)
            GroupChats.isNonemptyChatAdmin(adminId).shouldBeFalse()
        }

        test("An admin of a nonempty group chat should be the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, listOf(userId))
            GroupChats.isNonemptyChatAdmin(adminId).shouldBeTrue()
        }
    }

    context("queryUserChatEdges(String, String)") {
        test("Chats should be queried") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id, chat3Id) = (1..3).map {
                GroupChats.create(adminId)
            }
            val queryText = "hi"
            val (message1, message2) = listOf(chat1Id, chat2Id).map {
                val id = Messages.message(it, adminId, TextMessage(queryText))
                MessageEdge(Messages.read(id), cursor = id)
            }
            Messages.create(chat3Id, adminId, TextMessage("bye"))
            val chat1Edges = ChatEdges(chat1Id, listOf(message1))
            val chat2Edges = ChatEdges(chat2Id, listOf(message2))
            GroupChats.queryUserChatEdges(adminId, queryText) shouldBe listOf(chat1Edges, chat2Edges)
        }
    }

    context("search(String, String, BackwardPagination?)") {
        test("Chats should be searched case-insensitively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chats = listOf(
                NewGroupChat(GroupChatTitle("Title 1"), GroupChatDescription("")),
                NewGroupChat(GroupChatTitle("Title 2"), GroupChatDescription("")),
                NewGroupChat(GroupChatTitle("Iron Man Fan Club"), GroupChatDescription(""))
            )
            chats.forEach { GroupChats.create(adminId, it) }
            GroupChats.search(adminId, "itle ").map { it.title } shouldBe listOf(chats[0].title, chats[1].title)
            GroupChats.search(adminId, "iron").map { it.title } shouldBe listOf(chats[2].title)
        }
    }
})
