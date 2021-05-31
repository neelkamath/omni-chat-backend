package com.neelkamath.omniChatBackend.db.tables

import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.GroupChatId
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChat
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.UpdatedGroupChatPic
import com.neelkamath.omniChatBackend.graphql.routing.*
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.*

/** Returns the created chat's ID. It doesn't matter whether [adminIdList] and [userIdList] intersect. */
fun GroupChats.create(
    adminIdList: Collection<Int>,
    userIdList: Collection<Int> = listOf(),
    title: GroupChatTitle = GroupChatTitle("T"),
    description: GroupChatDescription = GroupChatDescription(""),
    isBroadcast: Boolean = false,
    publicity: GroupChatPublicity = GroupChatPublicity.NOT_INVITABLE,
): Int = create(
    GroupChatInput(
        title,
        description,
        userIdList = userIdList + adminIdList,
        adminIdList = adminIdList.toList(),
        isBroadcast = isBroadcast,
        publicity = publicity,
    )
)

@ExtendWith(DbExtension::class)
class GroupChatsTest {
    @Nested
    inner class Create {
        @Test
        fun `Creating a chat must only notify participants`(): Unit = runBlocking {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val (adminSubscriber, user1Subscriber, user2Subscriber) = listOf(adminId, user1Id, user2Id)
                .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            val chatId = GroupChats.create(listOf(adminId), listOf(user1Id))
            awaitBrokering()
            listOf(adminSubscriber, user1Subscriber).forEach { subscriber ->
                val actual = subscriber.values().map { (it as GroupChatId).getChatId() }
                assertEquals(listOf(chatId), actual)
            }
            user2Subscriber.assertNoValues()
        }
    }

    @Nested
    inner class UpdateTitle {
        @Test
        fun `Updating the title must only send a notification to participants`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId))
            awaitBrokering()
            val (adminSubscriber, userSubscriber) =
                listOf(adminId, userId).map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            val title = GroupChatTitle("New Title")
            GroupChats.updateTitle(chatId, title)
            awaitBrokering()
            val values = adminSubscriber.values().map { it as UpdatedGroupChat }
            assertEquals(listOf(chatId), values.map { it.getChatId() })
            assertEquals(listOf(title), values.map { it.getTitle() })
            userSubscriber.assertNoValues()
        }
    }

    @Nested
    inner class UpdateDescription {
        @Test
        fun `Updating the description must only notify participants`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
                val chatId = GroupChats.create(listOf(adminId))
                awaitBrokering()
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                    .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                val description = GroupChatDescription("New description.")
                GroupChats.updateDescription(chatId, description)
                awaitBrokering()
                val values = adminSubscriber.values().map { it as UpdatedGroupChat }
                assertEquals(listOf(chatId), values.map { it.getChatId() })
                assertEquals(listOf(description), values.map { it.getDescription() })
                userSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class UpdatePic {
        @Test
        fun `Updating the chat's pic must only notify subscribers`(): Unit = runBlocking {
            val (adminId, nonParticipantId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId))
            awaitBrokering()
            val (adminSubscriber, nonParticipantSubscriber) = listOf(adminId, nonParticipantId)
                .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            GroupChats.updatePic(chatId, readPic("76px×57px.jpg"))
            awaitBrokering()
            val actual = adminSubscriber.values().map { (it as UpdatedGroupChatPic).getChatId() }
            assertEquals(listOf(chatId), actual)
            nonParticipantSubscriber.assertNoValues()
        }

        @Test
        fun `The pic must get deleted`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            GroupChats.updatePic(chatId, readPic("76px×57px.jpg"))
            GroupChats.updatePic(chatId, pic = null)
            assertNull(GroupChats.readPic(chatId, PicType.ORIGINAL))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting a nonempty chat must throw an exception`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            assertFailsWith<IllegalArgumentException> { GroupChats.delete(chatId) }
        }

        @Test
        fun `Deleting a chat must wipe it from the DB`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            MessageStatuses.create(userId, messageId, MessageStatus.READ)
            TypingStatuses.update(chatId, adminId, isTyping = true)
            Stargazers.create(userId, messageId)
            GroupChatUsers.removeUsers(chatId, adminId, userId)
            listOf(Chats, GroupChats, GroupChatUsers, Messages, MessageStatuses, Stargazers, TypingStatuses)
                .forEach { assertEquals(0, it.count()) }
        }
    }

    @Nested
    inner class Search {
        @Test
        fun `Chats must be searched case-insensitively`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatIdList = listOf("Title 1", "Title 2", "Iron Man Fan Club")
                .map { GroupChats.create(listOf(adminId), title = GroupChatTitle(it)) }
            assertEquals(linkedHashSetOf(chatIdList[0], chatIdList[1]), GroupChats.search(adminId, "itle "))
            assertEquals(linkedHashSetOf(chatIdList[2]), GroupChats.search(adminId, "iron"))
        }
    }

    /** Creates [count] public chats, and returns their IDs in ascending order. */
    private fun createPublicChats(count: Int = 10): CreatedChats {
        val adminId = createVerifiedUsers(1).first().userId
        val chatIdList = (1..count)
            .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC) }
            .toLinkedHashSet()
        return CreatedChats(adminId, chatIdList)
    }

    /** The [chatIdList] group chat IDs sorted in ascending order. The only user in each chat is the [adminId]. */
    private data class CreatedChats(val adminId: Int, val chatIdList: LinkedHashSet<Int>)

    @Nested
    inner class SearchPublicChats {
        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (_, chatIdList) = createPublicChats()
            assertEquals(chatIdList, GroupChats.searchPublicChats(query = ""))
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (_, chatIdList) = createPublicChats()
            val first = 3
            val index = 4
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = GroupChats.searchPublicChats(query = "", pagination)
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (_, chatIdList) = createPublicChats()
            val first = 3
            val actual = GroupChats.searchPublicChats(query = "", ForwardPagination(first))
            assertEquals(chatIdList.take(first).toLinkedHashSet(), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (_, chatIdList) = createPublicChats()
            val index = 4
            val pagination = ForwardPagination(after = chatIdList.elementAt(index))
            val actual = GroupChats.searchPublicChats(query = "", pagination)
            assertEquals(chatIdList.drop(index + 1).toLinkedHashSet(), actual)
        }

        @Test
        fun `Zero items must be retrieved when using the last item's cursor`() {
            val (_, chatIdList) = createPublicChats()
            val pagination = ForwardPagination(after = chatIdList.last())
            assertEquals(0, GroupChats.searchPublicChats(query = "", pagination).size)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (adminId, chatIdList) = createPublicChats()
            GroupChatUsers.removeUsers(chatIdList.elementAt(3), adminId)
            val expected = listOf(2, 4, 5).map(chatIdList::elementAt).toLinkedHashSet()
            val pagination = ForwardPagination(first = 3, after = chatIdList.elementAt(1))
            assertEquals(expected, GroupChats.searchPublicChats(query = "", pagination))
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (adminId, chatIdList) = createPublicChats()
            val index = 4
            val chatId = chatIdList.elementAt(index)
            GroupChatUsers.removeUsers(chatId, adminId)
            val actual = GroupChats.searchPublicChats(query = "", ForwardPagination(after = chatId))
            assertEquals(chatIdList.drop(index + 1).toLinkedHashSet(), actual)
        }
    }

    @Nested
    inner class ReadPublicChatsCursor {
        @Test
        fun `If there are zero items, the 'startCursor' and 'endCursor' must indicate such`() {
            assertNull(GroupChats.readPublicChatsCursor(query = "", CursorType.START))
            assertNull(GroupChats.readPublicChatsCursor(query = "", CursorType.END))
        }

        @Test
        fun `If there's one item, the 'startCursor' and 'endCursor' must point to the one item`() {
            val cursor = createPublicChats(count = 1).chatIdList.first()
            assertEquals(cursor, GroupChats.readPublicChatsCursor(query = "", CursorType.START))
            assertEquals(cursor, GroupChats.readPublicChatsCursor(query = "", CursorType.END))
        }

        @Test
        fun `The start and end cursors must point to the first and last items respectively`() {
            val (_, chatIdList) = createPublicChats()
            assertEquals(chatIdList.first(), GroupChats.readPublicChatsCursor(query = "", CursorType.START))
            assertEquals(chatIdList.last(), GroupChats.readPublicChatsCursor(query = "", CursorType.END))
        }
    }

    @Nested
    inner class SetBroadcastStatus {
        @Test
        fun `Only participants must be notified of the status update`() {
            runBlocking {
                val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
                val chatId = GroupChats.create(listOf(adminId))
                awaitBrokering()
                val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                    .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
                val isBroadcast = true
                GroupChats.setBroadcastStatus(chatId, isBroadcast)
                awaitBrokering()
                val actual = adminSubscriber.values().map { it as UpdatedGroupChat }
                assertEquals(listOf(chatId), actual.map { it.getChatId() })
                assertEquals(listOf(isBroadcast), actual.map { it.getIsBroadcast() })
                userSubscriber.assertNoValues()
            }
        }
    }

    @Nested
    inner class SetInvitability {
        @Test
        fun `An exception must be thrown if the chat is public`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertFailsWith<IllegalArgumentException> { GroupChats.setInvitability(chatId, isInvitable = true) }
        }

        @Test
        fun `Only participants must be notified of the updated status`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(listOf(adminId))
            val (adminSubscriber, userSubscriber) = listOf(adminId, userId)
                .map { groupChatsNotifier.subscribe(UserId(it)).subscribeWith(TestSubscriber()) }
            GroupChats.setInvitability(chatId, isInvitable = true)
            awaitBrokering()
            val values = adminSubscriber.values().map { it as UpdatedGroupChat }
            assertEquals(listOf(chatId), values.map { it.getChatId() })
            assertEquals(listOf(GroupChatPublicity.INVITABLE), values.map { it.getPublicity() })
            userSubscriber.assertNoValues()
        }
    }

    @Nested
    inner class IsInvitable {
        @Test
        fun `A non-existing chat mustn't be invitable`(): Unit = assertFalse(GroupChats.isInvitable(chatId = 1))

        @Test
        fun `A chat disallowing invitations must be stated as such`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            assertFalse(GroupChats.isInvitable(chatId))
        }

        @Test
        fun `A chat allowing invites must state such`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            assertTrue(GroupChats.isInvitable(chatId))
        }
    }

    @Nested
    inner class IsExistingInviteCode {
        @Test
        fun `A chat with the specified invite code which is invitable must be stated as such`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val inviteCode = GroupChats.readInviteCode(chatId)!!
            assertTrue(GroupChats.isExistingInviteCode(inviteCode))
        }
    }

    @Nested
    inner class ReadInviteCode {
        @Test
        fun `The invite code must be retrieved for an invitable chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertNotNull(GroupChats.readInviteCode(chatId))
        }

        @Test
        fun `The invite code mustn't be retrieved for a noninvitable chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(listOf(adminId))
            assertNull(GroupChats.readInviteCode(chatId))
        }
    }

    @Nested
    inner class QueryUserChatEdges {
        @Test
        fun `Chats must be queried`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id, chat3Id) = (1..3).map { GroupChats.create(listOf(adminId)) }
            val queryText = "hi"
            val (message1Id, message2Id) =
                listOf(chat1Id, chat2Id).map { Messages.message(adminId, it, MessageText(queryText)) }
            Messages.message(adminId, chat3Id, MessageText("bye"))
            val chat1Edges = ChatEdges(chat1Id, linkedHashSetOf(message1Id))
            val chat2Edges = ChatEdges(chat2Id, linkedHashSetOf(message2Id))
            assertEquals(linkedHashSetOf(chat1Edges, chat2Edges), GroupChats.queryUserChatEdges(adminId, queryText))
        }
    }
}
