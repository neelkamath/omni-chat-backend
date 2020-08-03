package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import io.reactivex.rxjava3.subscribers.TestSubscriber

class GroupChatsTest : FunSpec({
    context("setBroadcastStatus(Int, Boolean)") {
        test("Only participants should be notified of the status update") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            val isBroadcast = true
            GroupChats.setBroadcastStatus(chatId, isBroadcast)
            adminSubscriber.assertValue(UpdatedGroupChat(chatId, isBroadcast = isBroadcast))
            userSubscriber.assertNoValues()
        }
    }

    context("create(GroupChatInput") {
        test("Creating a chat should only notify participants") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { newGroupChatsBroker.subscribe(NewGroupChatsAsset(it)).subscribeWith(TestSubscriber()) }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id))
            listOf(adminSubscriber, user1Subscriber).forEach { it.assertValue(GroupChatId(chatId)) }
            user2Subscriber.assertNoValues()
        }
    }

    context("updateTitle(Int, GroupChatTitle)") {
        test("Updating the title should only send a notification to participants") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            val title = GroupChatTitle("New Title")
            GroupChats.updateTitle(chatId, title)
            adminSubscriber.assertValue(UpdatedGroupChat(chatId, title))
            userSubscriber.assertNoValues()
        }
    }

    context("updateDescription(Int, GroupChatDescription)") {
        test("Updating the description should only notify participants") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            val description = GroupChatDescription("New description.")
            GroupChats.updateDescription(chatId, description)
            adminSubscriber.assertValue(UpdatedGroupChat(chatId, description = description))
            userSubscriber.assertNoValues()
        }
    }

    context("updatePic(Int, Pic?)") {
        test("Updating the chat's pic should notify subscribers") {
            val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                .map { updatedChatsBroker.subscribe(UpdatedChatsAsset(it)).subscribeWith(TestSubscriber()) }
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            GroupChats.updatePic(chatId, pic)
            adminSubscriber.assertValue(UpdatedGroupChat(chatId))
            nonParticipantSubscriber.assertNoValues()
        }
    }

    context("delete(Int)") {
        test("Deleting a nonempty chat should throw an exception") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            shouldThrowExactly<IllegalArgumentException> { GroupChats.delete(chatId) }
        }

        test("Deleting a chat should wipe it from the DB") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            MessageStatuses.create(userId, messageId, MessageStatus.READ)
            TypingStatuses.set(chatId, adminId, isTyping = true)
            Stargazers.create(userId, messageId)
            GroupChatUsers.removeUsers(chatId, adminId, userId)
            listOf(Chats, GroupChats, GroupChatUsers, Messages, MessageStatuses, Stargazers, TypingStatuses)
                .forEach { it.count().shouldBeZero() }
        }
    }

    context("queryUserChatEdges(Int, String)") {
        test("Chats should be queried") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id, chat3Id) = (1..3).map { GroupChats.create(listOf(adminId)) }
            val queryText = "hi"
            val (message1, message2) = listOf(chat1Id, chat2Id).map {
                val messageId = Messages.message(adminId, it, MessageText(queryText))
                MessageEdge(Messages.readMessage(adminId, messageId), cursor = messageId)
            }
            Messages.create(adminId, chat3Id, MessageText("bye"))
            val chat1Edges = ChatEdges(chat1Id, listOf(message1))
            val chat2Edges = ChatEdges(chat2Id, listOf(message2))
            GroupChats.queryUserChatEdges(adminId, queryText) shouldBe listOf(chat1Edges, chat2Edges)
        }
    }

    context("search(Int, String, ForwardPagination?, BackwardPagination?)") {
        test("Chats should be searched case-insensitively") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val titles = listOf("Title 1", "Title 2", "Iron Man Fan Club")
            titles.forEach {
                val chat = GroupChatInput(
                    GroupChatTitle(it),
                    GroupChatDescription(""),
                    userIdList = listOf(adminId),
                    adminIdList = listOf(adminId),
                    isBroadcast = false
                )
                GroupChats.create(chat)
            }
            GroupChats.search(adminId, "itle ").map { it.title.value } shouldBe listOf(titles[0], titles[1])
            GroupChats.search(adminId, "iron").map { it.title.value } shouldBe listOf(titles[2])
        }
    }
})