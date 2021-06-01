package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.ExitedUsers
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.GroupChatId
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChat
import com.neelkamath.omniChatBackend.graphql.routing.GroupChatPublicity
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

private fun GroupChatUsers.makeAdmins(chatId: Int, vararg userIdList: Int): Unit =
    makeAdmins(chatId, userIdList.toList())

@ExtendWith(DbExtension::class)
class GroupChatUsersTest {
    @Nested
    inner class MakeAdmins {
        @Test
        fun `Setting the admin to a user who isn't in the chat must throw an exception`() {
            val (adminId, invalidAdminId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertFailsWith<IllegalArgumentException> { GroupChatUsers.makeAdmins(chatId, invalidAdminId) }
        }

        @Test
        fun `Only the specified users must be made admins`() {
            val (adminId, user1Id, user2Id, user3Id) = createVerifiedUsers(4).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(user1Id, user2Id, user3Id))
            GroupChatUsers.makeAdmins(chatId, user1Id, user2Id)
            assertTrue(GroupChatUsers.isAdmin(user1Id, chatId))
            assertTrue(GroupChatUsers.isAdmin(user2Id, chatId))
            assertFalse(GroupChatUsers.isAdmin(user3Id, chatId))
        }

        @Test
        fun `Making admins must only notify participants and unauthenticated subscribers`() {
            runBlocking {
                val (adminId, toBeAdminId, nonParticipantId) = createVerifiedUsers(3).map { it.userId }
                val chatId = GroupChats.create(setOf(adminId), userIdList = setOf(toBeAdminId))
                awaitBrokering()
                val (adminSubscriber, toBeAdminSubscriber, nonParticipantSubscriber) =
                    setOf(adminId, toBeAdminId, nonParticipantId)
                        .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                val unauthenticatedSubscriber =
                    groupChatMetadataNotifier.subscribe(ChatId(chatId)).subscribeWith(TestSubscriber())
                GroupChatUsers.makeAdmins(chatId, toBeAdminId)
                awaitBrokering()
                setOf(adminSubscriber, toBeAdminSubscriber, unauthenticatedSubscriber).forEach { subscriber ->
                    val values = subscriber.values().map { it as UpdatedGroupChat }
                    assertEquals(listOf(chatId), values.map { it.getChatId() })
                    val expected = listOf(listOf(adminId, toBeAdminId))
                    assertEquals(expected, values.map { it.getAdminIdList() })
                }
                nonParticipantSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class ReadFellowParticipantIdList {
        @Test
        fun `Reading fellow participants must only include other participants in the chats`() {
            val (adminId, participantId) = createVerifiedUsers(2).map { it.userId }
            GroupChats.create(setOf(adminId), setOf(participantId))
            assertEquals(setOf(participantId), GroupChatUsers.readFellowParticipantIdList(adminId))
        }
    }

    @Nested
    inner class IsAdmin {
        @Test
        fun `A non-admin participating in the chat mustn't be said to be an admin`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            assertFalse(GroupChatUsers.isAdmin(userId, chatId))
        }

        @Test
        fun `A non-participant mustn't be said to be an admin`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertFalse(GroupChatUsers.isAdmin(userId, chatId))
        }

        @Test
        fun `The admin of a chat must have such stated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertTrue(GroupChatUsers.isAdmin(adminId, chatId))
        }
    }

    /**
     * The group [chatId] containing the [userIdList] which is the ID of each user in the chat sorted in ascending
     * order.
     */
    private data class CreatedGroupChat(val chatId: Int, val userIdList: LinkedHashSet<Int>)

    @Nested
    inner class ReadUserIdList {
        /** The [count] of the [CreatedGroupChat.userIdList] must be greater than zero. */
        private fun createGroupChat(count: Int = 10): CreatedGroupChat {
            val userIdList = createVerifiedUsers(count).map { it.userId }.toLinkedHashSet()
            val adminIdList = setOf(userIdList.first())
            val chatId = GroupChats.create(adminIdList, userIdList)
            return CreatedGroupChat(chatId, userIdList)
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (chatId, userIdList) = createGroupChat()
            assertEquals(userIdList, GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (chatId, userIdList) = createGroupChat()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = userIdList.elementAt(index))
            assertEquals(userIdList.slice(index + 1..index + first), GroupChatUsers.readUserIdList(chatId, pagination))
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (chatId, userIdList) = createGroupChat()
            val first = 3
            val actual = GroupChatUsers.readUserIdList(chatId, ForwardPagination(first = 3))
            assertEquals(userIdList.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (chatId, userIdList) = createGroupChat()
            val index = 3
            val pagination = ForwardPagination(after = userIdList.elementAt(index))
            val actual = GroupChatUsers.readUserIdList(chatId, pagination)
            assertEquals(userIdList.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val (chatId, userIdList) = createGroupChat()
            val pagination = ForwardPagination(after = userIdList.last())
            assertEquals(0, GroupChatUsers.readUserIdList(chatId, pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (chatId, userIdList) = createGroupChat()
            GroupChatUsers.removeUsers(chatId, userIdList.elementAt(3))
            val expected = listOf(2, 4, 5).map(userIdList::elementAt).toLinkedHashSet()
            val pagination = ForwardPagination(first = 3, after = userIdList.elementAt(1))
            assertEquals(expected, GroupChatUsers.readUserIdList(chatId, pagination))
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (chatId, userIdList) = createGroupChat()
            val index = 3
            val userId = userIdList.elementAt(index)
            GroupChatUsers.removeUsers(chatId, userId)
            val actual = GroupChatUsers.readUserIdList(chatId, ForwardPagination(after = userId))
            assertEquals(userIdList.drop(index + 1).toLinkedHashSet(), actual)
        }
    }

    @Nested
    inner class AddUsers {
        @Test
        fun `Users must be added to the chat while ignoring duplicates and participants`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(user1Id))
            GroupChatUsers.addUsers(chatId, user1Id, user2Id, user2Id)
            assertEquals(setOf(adminId, user1Id, user2Id), GroupChatUsers.readUserIdList(chatId).toSet())
        }

        @Test
        fun `New users must be notified of the chat, existing participants of the addition, and unauthenticated subscribers of the addition`(): Unit =
            runBlocking {
                val (admin, user) = createVerifiedUsers(2)
                val chatId = GroupChats.create(setOf(admin.userId), publicity = GroupChatPublicity.PUBLIC)
                awaitBrokering()
                val (adminSubscriber, userSubscriber) = setOf(admin.userId, user.userId)
                    .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                val unauthenticatedSubscriber =
                    groupChatMetadataNotifier.subscribe(ChatId(chatId)).subscribeWith(TestSubscriber())
                GroupChatUsers.addUsers(chatId, user.userId)
                awaitBrokering()
                setOf(adminSubscriber, unauthenticatedSubscriber).forEach { subscriber ->
                    val values = subscriber.values().map { it as UpdatedGroupChat }
                    assertEquals(listOf(chatId), values.map { it.getChatId() })
                    val expected = listOf(listOf(user.userId))
                    val actual = values.map { value ->
                        value.getNewUsers()!!.map { it.id }
                    }
                    assertEquals(expected, actual)
                }
                assertEquals(listOf(chatId), userSubscriber.values().map { (it as GroupChatId).getChatId() })
            }
    }

    @Nested
    inner class AddUserViaInviteCode {
        @Test
        fun `An exception must get thrown when the user is getting added to a non-invitable chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val inviteCode = GroupChats.readInviteCode(chatId)!!
            GroupChats.setInvitability(chatId, isInvitable = false)
            assertFailsWith<IllegalArgumentException> { GroupChatUsers.addUserViaInviteCode(adminId, inviteCode) }
        }
    }

    @Nested
    inner class CanUsersLeave {
        @Test
        fun `Checking if users who aren't in the chat can leave must return true`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertTrue(GroupChatUsers.canUsersLeave(chatId, userId))
        }

        @Test
        fun `The users must be able to leave if the chat will be empty`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertTrue(GroupChatUsers.canUsersLeave(chatId, adminId, userId))
        }

        @Test
        fun `The users mustn't be able to leave if the chat will have users sans admins`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertFalse(GroupChatUsers.canUsersLeave(chatId, adminId))
        }

        @Test
        fun `A non-admin must be able to leave`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertTrue(GroupChatUsers.canUsersLeave(chatId, userId))
        }

        @Test
        fun `An admin must be able to leave if there's another admin`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(admin1Id, admin2Id))
            assertTrue(GroupChatUsers.canUsersLeave(chatId, admin1Id))
        }
    }

    @Nested
    inner class RemoveUsers {
        @Test
        fun `Unauthenticated users must get unsubscribed only when the chat gets deleted`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val subscribers = setOf(
                chatMessagesNotifier,
                chatOnlineStatusesNotifier,
                chatTypingStatusesNotifier,
                chatAccountsNotifier,
                groupChatMetadataNotifier,
            ).map { it.subscribe(ChatId(chatId)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.removeUsers(chatId, userId)
            awaitBrokering()
            subscribers.forEach { it.assertNoValues() }
            GroupChatUsers.removeUsers(chatId, adminId)
            awaitBrokering()
            subscribers.forEach { it.assertComplete() }
        }

        @Test
        fun `An exception must be thrown if the users can't leave the chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertFailsWith<IllegalArgumentException> { GroupChatUsers.removeUsers(chatId, adminId) }
        }

        @Test
        fun `Users must be removed from the chat returning false`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertFalse(GroupChatUsers.removeUsers(chatId, userId))
            assertEquals(setOf(adminId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `Only one notification must be sent even if the user was removed twice`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            awaitBrokering()
            val (adminSubscriber, userSubscriber) =
                listOf(adminId, userId).map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            GroupChatUsers.removeUsers(chatId, userId, userId)
            awaitBrokering()
            listOf(adminSubscriber, userSubscriber).forEach { subscriber ->
                val values = subscriber.values().map { it as ExitedUsers }
                assertEquals(listOf(chatId), values.map { it.getChatId() })
                val expected = listOf(listOf(userId))
                assertEquals(expected, values.map { it.getUserIdList() })
            }
        }

        @Test
        fun `Only participants and unauthenticated subscribers must get notified`(): Unit = runBlocking {
            val (adminId, userId, nonParticipantId) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId), publicity = GroupChatPublicity.PUBLIC)
            awaitBrokering()
            val nonParticipantSubscriber =
                groupChatsNotifier.subscribe(UserId(nonParticipantId)).subscribeWith(TestSubscriber())
            val unauthenticatedSubscriber =
                groupChatMetadataNotifier.subscribe(ChatId(chatId)).subscribeWith(TestSubscriber())
            GroupChatUsers.removeUsers(chatId, userId)
            awaitBrokering()
            nonParticipantSubscriber.assertNoValues()
            val actual = unauthenticatedSubscriber.values().map { (it as ExitedUsers).getChatId() }
            assertEquals(listOf(chatId), actual)
        }

        @Test
        fun `Removing every user must delete the chat returning true`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertTrue(GroupChatUsers.removeUsers(chatId, adminId, userId))
            assertEquals(0, GroupChats.count())
        }

        @Test
        fun `Messages the removed user had starred in the chat must be unstarred for them`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val messageId = Messages.message(userId, chatId)
            Stargazers.create(userId, messageId)
            GroupChatUsers.removeUsers(chatId, userId)
            assertFalse(Stargazers.hasStar(userId, messageId))
        }
    }

    @Nested
    inner class CanUserLeave {
        @Test
        fun `The user must be able to leave if they aren't the last admin of a chat with other users in it`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            GroupChats.create(setOf(adminId), listOf(userId))
            assertTrue(GroupChatUsers.canUserLeave(userId))
        }

        @Test
        fun `The user mustn't be able to leave if they're the last admin of a chat with other users in it`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            GroupChats.create(setOf(adminId), listOf(userId))
            assertFalse(GroupChatUsers.canUserLeave(adminId))
        }
    }
}
