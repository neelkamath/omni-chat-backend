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

class GroupChatsTest : FunSpec({
    context("create(String, NewGroupChat") {
        test("A chat should be created which includes the admin in the list of users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            GroupChats.readChat(chatId).users.edges.map { it.node.id } shouldContainExactlyInAnyOrder
                    listOf(adminId, userId)
        }

        test("Creating a chat should notify only non-admins in the chat, even if the admin is in the user ID list") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { newGroupChatsBroker.subscribe(NewGroupChatsAsset(it)).subscribeWith(TestSubscriber()) }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(adminId, user1Id))
            user1Subscriber.assertValue(GroupChatId(chatId))
            listOf(adminSubscriber, user2Subscriber).forEach { it.assertNoValues() }
        }
    }

    context("setAdmin(Int, String)") {
        test("The new admin should be set") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            GroupChats.setAdmin(chatId, userId)
            GroupChats.isAdmin(userId, chatId).shouldBeTrue()
        }

        test("Setting the admin to a user who isn't in the chat should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId)
            shouldThrowExactly<IllegalArgumentException> { GroupChats.setAdmin(chatId, "new admin ID") }
        }
    }

    context("update(GroupChatUpdate)") {
        test("The chat should be deleted once every user has left it") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userIdList))
            GroupChats.update(GroupChatUpdate(chatId, removedUserIdList = userIdList + adminId))
            GroupChats.count().shouldBeZero()
        }

        test("A subscriber should be notified when the chat is updated") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId)
            val subscriber = updatedChatsBroker.subscribe(UpdatedChatsAsset(adminId)).subscribeWith(TestSubscriber())
            val update = GroupChatUpdate(chatId, GroupChatTitle("New Title"), newUserIdList = listOf(userId))
            GroupChats.update(update)
            subscriber.assertValue(update.toUpdatedGroupChat())
        }

        test("Only the new users in the update should be notified they were added") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(user1Id))
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { newGroupChatsBroker.subscribe(NewGroupChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChats.update(GroupChatUpdate(chatId, newUserIdList = listOf(user1Id, user2Id)))
            listOf(adminSubscriber, user1Subscriber).forEach { it.assertNoValues() }
            user2Subscriber.assertValue(GroupChatId(chatId))
        }
    }

    context("updatePic(Int, ByteArray?)") {
        test("Updating the chat's pic should notify subscribers") {
            val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId)
            val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            GroupChats.updatePic(chatId, readImage("31kB.png"))
            adminSubscriber.assertValue(UpdatedGroupChat(chatId))
            nonParticipantSubscriber.assertNoValues()
        }
    }

    context("removeUsers(Int, List<String>)") {
        test("Messages from a user who left should be retained") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val messageId = Messages.message(chatId, userId, TextMessage("t"))
            GroupChatUsers.removeUsers(chatId, userId)
            Messages.readIdList(chatId) shouldBe listOf(messageId)
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
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            val messageId = Messages.message(chatId, adminId, TextMessage("t"))
            MessageStatuses.create(messageId, userId, MessageStatus.READ)
            GroupChatUsers.removeUsers(chatId, adminId, userId)
            GroupChats.count().shouldBeZero()
            Messages.count().shouldBeZero()
            MessageStatuses.count().shouldBeZero()
        }
    }

    context("isNonemptyChatAdmin(String)") {
        test("A non-admin shouldn't be the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, buildNewGroupChat(userId))
            GroupChats.isNonemptyChatAdmin(userId).shouldBeFalse()
        }

        test("An admin of an empty group chat shouldn't be the admin of a nonempty group chat") {
            val adminId = createVerifiedUsers(1)[0].info.id
            GroupChats.create(adminId)
            GroupChats.isNonemptyChatAdmin(adminId).shouldBeFalse()
        }

        test("An admin of a nonempty group chat should be the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, buildNewGroupChat(userId))
            GroupChats.isNonemptyChatAdmin(adminId).shouldBeTrue()
        }
    }

    context("queryUserChatEdges(String, String)") {
        test("Chats should be queried") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id, chat3Id) = (1..3).map { GroupChats.create(adminId) }
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