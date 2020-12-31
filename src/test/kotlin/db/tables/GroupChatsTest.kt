package com.neelkamath.omniChat.db.tables

import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.routing.*
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

@ExtendWith(DbExtension::class)
class GroupChatsTest {
    @Nested
    inner class SetBroadcastStatus {
        @Test
        fun `Only participants should be notified of the status update`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId))
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                        .map { groupChatsNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                val isBroadcast = true
                GroupChats.setBroadcastStatus(chatId, isBroadcast)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedGroupChat(chatId, isBroadcast = isBroadcast))
                userSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `Creating a chat should only notify participants`() {
            runBlocking {
                val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
                val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id).map {
                    groupChatsNotifier.safelySubscribe(it).subscribeWith(TestSubscriber())
                }
                val chatId = GroupChats.create(listOf(adminId), listOf(user1Id))
                awaitBrokering()
                listOf(adminSubscriber, user1Subscriber).forEach { it.assertValue(GroupChatId(chatId)) }
                user2Subscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class UpdateTitle {
        @Test
        fun `Updating the title should only send a notification to participants`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId))
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                        .map { groupChatsNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                val title = GroupChatTitle("New Title")
                GroupChats.updateTitle(chatId, title)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedGroupChat(chatId, title))
                userSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class UpdateDescription {
        @Test
        fun `Updating the description should only notify participants`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId))
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                        .map { groupChatsNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                val description = GroupChatDescription("New description.")
                GroupChats.updateDescription(chatId, description)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedGroupChat(chatId, description = description))
                userSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class UpdatePic {
        @Test
        fun `Updating the chat's pic should notify subscribers`() {
            runBlocking {
                val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId))
                val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                        .map { groupChatsNotifier.safelySubscribe(it).subscribeWith(TestSubscriber()) }
                val pic = Pic(ByteArray(1), Pic.Type.PNG)
                GroupChats.updatePic(chatId, pic)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedGroupChat(chatId))
                nonParticipantSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting a nonempty chat should throw an exception`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertFailsWith<IllegalArgumentException> { GroupChats.delete(chatId) }
        }

        @Test
        fun `Deleting a chat should wipe it from the DB`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            MessageStatuses.create(userId, messageId, MessageStatus.READ)
            TypingStatuses.set(chatId, adminId, isTyping = true)
            Stargazers.create(userId, messageId)
            GroupChatUsers.removeUsers(chatId, adminId, userId)
            listOf(Chats, GroupChats, GroupChatUsers, Messages, MessageStatuses, Stargazers, TypingStatuses)
                    .forEach { assertEquals(0, it.count()) }
        }
    }

    @Nested
    inner class QueryUserChatEdges {
        @Test
        fun `Chats should be queried`() {
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
            assertEquals(listOf(chat1Edges, chat2Edges), GroupChats.queryUserChatEdges(adminId, queryText))
        }
    }

    @Nested
    inner class Search {
        @Test
        fun `Chats should be searched case-insensitively`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val titles = listOf("Title 1", "Title 2", "Iron Man Fan Club")
            titles.forEach { GroupChats.create(listOf(adminId), title = GroupChatTitle(it)) }
            assertEquals(listOf(titles[0], titles[1]), GroupChats.search(adminId, "itle ").map { it.title.value })
            assertEquals(listOf(titles[2]), GroupChats.search(adminId, "iron").map { it.title.value })
        }
    }

    @Nested
    inner class SetInvitability {
        @Test
        fun `An exception should be thrown if the chat is public`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertFailsWith<IllegalArgumentException> { GroupChats.setInvitability(chatId, isInvitable = true) }
        }

        @Test
        fun `Only participants should be notified of the updated status`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
                val chatId = GroupChats.create(listOf(adminId))
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                        .map { groupChatsNotifier.subscribe(it).subscribeWith(TestSubscriber()) }
                GroupChats.setInvitability(chatId, isInvitable = true)
                awaitBrokering()
                adminSubscriber.assertValue(UpdatedGroupChat(chatId, publicity = GroupChatPublicity.INVITABLE))
                userSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class IsInvitable {
        @Test
        fun `A nonexistent chat shouldn't be invitable`() {
            assertFalse(GroupChats.isInvitable(chatId = 1))
        }

        @Test
        fun `A chat disallowing invitations should be stated as such`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertFalse(GroupChats.isInvitable(chatId))
        }

        @Test
        fun `A chat allowing invites should state such`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            assertTrue(GroupChats.isInvitable(chatId))
        }
    }
}
