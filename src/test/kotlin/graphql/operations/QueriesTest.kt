package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import io.ktor.http.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.collections.component4
import kotlin.test.*

const val SEARCH_BLOCKED_USERS_QUERY = """
    query SearchBlockedUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchBlockedUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

fun searchBlockedUsers(userId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = executeGraphQlViaEngine(
        SEARCH_BLOCKED_USERS_QUERY,
        mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId,
    ).data!!["searchBlockedUsers"]
    return testingObjectMapper.convertValue(data!!)
}

const val READ_TYPING_USERS_QUERY = """
    query ReadTypingUsers {
        readTypingUsers {
            $TYPING_USERS_FRAGMENT
        }
    }
"""

fun readTypingUsers(userId: Int): List<TypingUsers> {
    val data = executeGraphQlViaEngine(READ_TYPING_USERS_QUERY, userId = userId).data!!["readTypingUsers"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_BLOCKED_USERS_QUERY = """
    query ReadBlockedUsers(${"$"}first: Int, ${"$"}after: Cursor) {
        readBlockedUsers(first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""


fun readBlockedUsers(userId: Int, pagination: ForwardPagination? = null): AccountsConnection {
    val data = executeGraphQlViaEngine(
        READ_BLOCKED_USERS_QUERY,
        mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId,
    ).data!!["readBlockedUsers"]!!
    return testingObjectMapper.convertValue(data)
}

const val SEARCH_PUBLIC_CHATS_QUERY = """
    query SearchPublicChats(
        ${"$"}query: String!
        ${"$"}first: Int
        ${"$"}after: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchPublicChats(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $GROUP_CHATS_CONNECTION_FRAGMENT
        }
    }
"""

fun searchPublicChats(
    query: String,
    pagination: ForwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null,
): GroupChatsConnection {
    val data = executeGraphQlViaEngine(
        SEARCH_PUBLIC_CHATS_QUERY,
        mapOf(
            "query" to query,
            "first" to pagination?.first,
            "after" to pagination?.after?.toString(),
            "groupChat_users_first" to usersPagination?.first,
            "groupChat_users_after" to usersPagination?.after?.toString(),
            "groupChat_messages_last" to messagesPagination?.last,
            "groupChat_messages_before" to messagesPagination?.before?.toString(),
        ),
    ).data!!["searchPublicChats"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_GROUP_CHAT_QUERY = """
    query ReadGroupChat(
        ${"$"}inviteCode: Uuid!
        ${"$"}groupChatInfo_users_first: Int
        ${"$"}groupChatInfo_users_after: Cursor
    ) {
        readGroupChat(inviteCode: ${"$"}inviteCode) {
            $READ_GROUP_CHAT_RESULT_FRAGMENT
        }
    }
"""

fun readGroupChat(inviteCode: UUID, usersPagination: ForwardPagination? = null): ReadGroupChatResult {
    val data = executeGraphQlViaEngine(
        READ_GROUP_CHAT_QUERY,
        mapOf(
            "inviteCode" to inviteCode.toString(),
            "groupChatInfo_users_first" to usersPagination?.first,
            "groupChatInfo_users_after" to usersPagination?.after?.toString(),
        ),
    ).data!!["readGroupChat"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_STARS_QUERY = """
    query ReadStars(${"$"}first: Int, ${"$"}after: Cursor) {
        readStars(first: ${"$"}first, after: ${"$"}after) {
            $STARRED_MESSAGES_CONNECTION_FRAGMENT
        }
    }
"""

fun readStars(userId: Int, pagination: ForwardPagination? = null): StarredMessagesConnection {
    val data = executeGraphQlViaEngine(
        READ_STARS_QUERY, mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId,
    ).data!!["readStars"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_ONLINE_STATUS_QUERY = """
    query ReadOnlineStatus(${"$"}userId: Int!) {
        readOnlineStatus(userId: ${"$"}userId) {
            $READ_ONLINE_STATUS_FRAGMENT
        }
    }
"""

fun readOnlineStatus(userId: Int): ReadOnlineStatusResult {
    val data = executeGraphQlViaEngine(READ_ONLINE_STATUS_QUERY, mapOf("userId" to userId))
        .data!!["readOnlineStatus"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_ACCOUNT_QUERY = """
    query ReadAccount {
        readAccount {
            $ACCOUNT_FRAGMENT
        }
    }
"""

fun readAccount(userId: Int): Account {
    val data = executeGraphQlViaEngine(READ_ACCOUNT_QUERY, userId = userId).data!!["readAccount"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_CHATS_QUERY = """
    query ReadChats(
        ${"$"}first: Int,
        ${"$"}after: Cursor,
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChats(first: ${"$"}first, after: ${"$"}after) {
            $CHATS_CONNECTION_FRAGMENT
        }
    }
"""

fun readChats(
    userId: Int,
    pagination: ForwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
): ChatsConnection {
    val chats = executeGraphQlViaEngine(
        READ_CHATS_QUERY,
        mapOf(
            "first" to pagination?.first,
            "after" to pagination?.after?.toString(),
            "privateChat_messages_last" to privateChatMessagesPagination?.last,
            "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
            "groupChat_users_first" to usersPagination?.first,
            "groupChat_users_after" to usersPagination?.after?.toString(),
            "groupChat_messages_last" to groupChatMessagesPagination?.last,
            "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString(),
        ),
        userId,
    ).data!!["readChats"]!!
    return testingObjectMapper.convertValue(chats)
}

const val READ_CHAT_QUERY = """
    query ReadChat(
        ${"$"}id: Int!
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChat(id: ${"$"}id) {
            $READ_CHAT_RESULT_FRAGMENT
        }
    }
"""

fun readChat(
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
    userId: Int? = null,
): ReadChatResult {
    val data = executeGraphQlViaEngine(
        READ_CHAT_QUERY,
        mapOf(
            "id" to id,
            "privateChat_messages_last" to privateChatMessagesPagination?.last,
            "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
            "groupChat_users_first" to usersPagination?.first,
            "groupChat_users_after" to usersPagination?.after?.toString(),
            "groupChat_messages_last" to groupChatMessagesPagination?.last,
            "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString(),
        ),
        userId,
    ).data!!["readChat"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_CONTACTS_QUERY = """
    query ReadContacts(${"$"}first: Int, ${"$"}after: Cursor) {
        readContacts(first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

fun readContacts(userId: Int, pagination: ForwardPagination? = null): AccountsConnection {
    val data = executeGraphQlViaEngine(
        READ_CONTACTS_QUERY,
        mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId,
    ).data!!["readContacts"]!!
    return testingObjectMapper.convertValue(data)
}

const val REFRESH_TOKEN_SET_QUERY = """
    query RefreshTokenSet(${"$"}refreshToken: ID!) {
        refreshTokenSet(refreshToken: ${"$"}refreshToken) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

fun refreshTokenSet(refreshToken: String): TokenSet {
    val data = executeGraphQlViaEngine(REFRESH_TOKEN_SET_QUERY, mapOf("refreshToken" to refreshToken))
        .data!!["refreshTokenSet"]!!
    return testingObjectMapper.convertValue(data)
}

const val REQUEST_TOKEN_SET_QUERY = """
    query RequestTokenSet(${"$"}login: Login!) {
        requestTokenSet(login: ${"$"}login) {
            $REQUEST_TOKEN_SET_RESULT_FRAGMENT
        }
    }
"""

fun requestTokenSet(login: Login): RequestTokenSetResult {
    val data = executeGraphQlViaEngine(REQUEST_TOKEN_SET_QUERY, mapOf("login" to login))
        .data!!["requestTokenSet"]!!
    return testingObjectMapper.convertValue(data)
}

const val SEARCH_CHAT_MESSAGES_QUERY = """
    query SearchChatMessages(${"$"}chatId: Int!, ${"$"}query: String!, ${"$"}last: Int, ${"$"}before: Cursor) {
        searchChatMessages(chatId: ${"$"}chatId, query: ${"$"}query, last: ${"$"}last, before: ${"$"}before) {
            $SEARCH_CHAT_MESSAGES_RESULT_FRAGMENT
        }
    }
"""

fun searchChatMessages(
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null,
    userId: Int? = null,
): SearchChatMessagesResult {
    val data = executeGraphQlViaEngine(
        SEARCH_CHAT_MESSAGES_QUERY,
        mapOf(
            "chatId" to chatId,
            "query" to query,
            "last" to pagination?.last,
            "before" to pagination?.before?.toString(),
        ),
        userId,
    ).data!!["searchChatMessages"]!!
    return testingObjectMapper.convertValue(data)
}

const val SEARCH_CHATS_QUERY = """
    query SearchChats(
        ${"$"}query: String!
        ${"$"}first: Int
        ${"$"}after: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchChats(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $CHATS_CONNECTION_FRAGMENT
        }
    }
"""

fun searchChats(
    userId: Int,
    query: String,
    pagination: ForwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
): ChatsConnection {
    val chats = executeGraphQlViaEngine(
        SEARCH_CHATS_QUERY,
        mapOf(
            "query" to query,
            "first" to pagination?.first,
            "after" to pagination?.after?.toString(),
            "privateChat_messages_last" to privateChatMessagesPagination?.last,
            "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
            "groupChat_users_first" to usersPagination?.first,
            "groupChat_users_after" to usersPagination?.after?.toString(),
            "groupChat_messages_last" to groupChatMessagesPagination?.last,
            "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
        ),
        userId,
    ).data!!["searchChats"]!!
    return testingObjectMapper.convertValue(chats)
}

const val SEARCH_CONTACTS_QUERY = """
    query SearchContacts(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchContacts(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

fun searchContacts(userId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = executeGraphQlViaEngine(
        SEARCH_CONTACTS_QUERY,
        mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId,
    ).data!!["searchContacts"]!!
    return testingObjectMapper.convertValue(data)
}

const val SEARCH_MESSAGES_QUERY = """
    query SearchMessages(
        ${"$"}query: String!
        ${"$"}first: Int,
        ${"$"}after: Cursor,
        ${"$"}chatMessages_messages_last: Int
        ${"$"}chatMessages_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchMessages(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $CHAT_MESSAGES_CONNECTION_FRAGMENT
        }
    }
"""

fun searchMessages(
    userId: Int,
    query: String,
    pagination: ForwardPagination? = null,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
): ChatMessagesConnection {
    val messages = executeGraphQlViaEngine(
        SEARCH_MESSAGES_QUERY,
        mapOf(
            "query" to query,
            "first" to pagination?.first,
            "after" to pagination?.after?.toString(),
            "chatMessages_messages_last" to chatMessagesPagination?.last,
            "chatMessages_messages_before" to chatMessagesPagination?.before?.toString(),
            "privateChat_messages_last" to privateChatMessagesPagination?.last,
            "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
            "groupChat_users_first" to usersPagination?.first,
            "groupChat_users_after" to usersPagination?.after?.toString(),
            "groupChat_messages_last" to groupChatMessagesPagination?.last,
            "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
        ),
        userId,
    ).data!!["searchMessages"]!!
    return testingObjectMapper.convertValue(messages)
}

const val SEARCH_USERS_QUERY = """
    query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

fun searchUsers(query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = executeGraphQlViaEngine(
        SEARCH_USERS_QUERY,
        mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
    ).data!!["searchUsers"]!!
    return testingObjectMapper.convertValue(data)
}

@ExtendWith(DbExtension::class)
class ChatMessagesEdgeDtoTest {
    @Nested
    inner class ReadGroupChat {
        @Test
        fun `The chat's info must be read`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val inviteCode = GroupChats.readInviteCode(chatId)
            assertEquals(GroupChats.readChatInfo(inviteCode), readGroupChat(inviteCode))
        }

        @Test
        fun `Reading a chat using a nonexistent invite code must fail`(): Unit =
            assertTrue(readGroupChat(UUID.randomUUID()) is InvalidInviteCode)
    }

    /** Data on a group chat having only ever contained an admin. */
    private data class AdminMessages(
        /** The ID of the chat's admin. */
        val adminId: Int,
        /** Every message sent has this text. */
        val text: MessageText,
        /** The ten messages the admin sent in the order of their creation. */
        val messageIdList: LinkedHashSet<Int>,
    )

    @Nested
    inner class GetMessages {
        private fun createUtilizedChat(): AdminMessages {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message = MessageText("t")
            val messageIdList = (1..10).map { Messages.message(adminId, chatId, message) }.toLinkedHashSet()
            return AdminMessages(adminId, message, messageIdList)
        }

        private fun testPagination(mustDeleteMessage: Boolean) {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val index = 5
            if (mustDeleteMessage) Messages.delete(messageIdList.elementAt(index))
            val last = 3
            val cursors = searchMessages(
                adminId,
                queryText.value,
                chatMessagesPagination = BackwardPagination(last, before = messageIdList.elementAt(index)),
            ).edges.flatMap { it.node.messages }.map { it.cursor }
            assertEquals(messageIdList.take(index).takeLast(last), cursors)
        }

        @Test
        fun `Messages must paginate using a cursor from a deleted message as if the message still exists`(): Unit =
            testPagination(mustDeleteMessage = true)

        @Test
        fun `Only the messages specified by the cursor and limit must be retrieved`(): Unit =
            testPagination(mustDeleteMessage = false)

        @Test
        fun `If neither cursor nor limit are supplied, every message must be retrieved`() {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val cursors = searchMessages(adminId, queryText.value).edges.flatMap { it.node.messages }.map { it.cursor }
            assertEquals(messageIdList, cursors.toSet())
        }
    }
}

@ExtendWith(DbExtension::class)
class QueriesTest {
    @Nested
    inner class SearchBlockedUsers {
        @Test
        fun `Blocked users must be searched`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            val actual = searchBlockedUsers(blockerId, query = "").edges.map { it.node.id }
            assertEquals(listOf(blockedId), actual)
        }

        @Test
        fun `Results must be paginated`(): Unit =
            testBlockedUsersPagination(BlockedUsersOperationName.SEARCH_BLOCKED_USERS)
    }

    @Nested
    inner class ReadTypingUsers {
        @Test
        fun `Typing statuses from the user's chats must be read excluding the user's own`() {
            val (adminId, participant1Id, participant2Id, nonParticipantId) = createVerifiedUsers(4).map { it.info.id }
            val groupChatId = GroupChats.create(listOf(adminId), listOf(participant1Id, participant2Id))
            val privateChatId = PrivateChats.create(participant2Id, nonParticipantId)
            TypingStatuses.update(groupChatId, adminId, isTyping = true)
            TypingStatuses.update(groupChatId, participant1Id, isTyping = true)
            TypingStatuses.update(privateChatId, nonParticipantId, isTyping = true)
            val users = listOf(Users.read(participant1Id).toAccount())
            val expected = listOf(TypingUsers(groupChatId, users))
            assertEquals(expected, readTypingUsers(adminId))
        }
    }

    @Nested
    inner class ReadBlockedUsers {
        @Test
        fun `Blocked users must be paginated`(): Unit =
            testBlockedUsersPagination(BlockedUsersOperationName.READ_BLOCKED_USERS)
    }

    @Nested
    inner class SearchPublicChats {
        @Test
        fun `Chats must be case-insensitively queried by their title`() {
            val adminId = createVerifiedUsers(1).first().info.id
            GroupChats.create(listOf(adminId), title = GroupChatTitle("Kotlin/Native"))
            val chatId = GroupChats
                .create(listOf(adminId), title = GroupChatTitle("Kotlin/JS"), publicity = GroupChatPublicity.PUBLIC)
            GroupChats.create(listOf(adminId), title = GroupChatTitle("Gaming"), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(listOf(chatId), searchPublicChats("kotlin").edges.map { it.node.id })
        }

        @Test
        fun `Chats must be paginated`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatIdList = (1..10).map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC) }
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = searchPublicChats(query = "", pagination).edges.map { it.node.id }
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }
    }

    @Nested
    inner class ReadStars {
        @Test
        fun `Only the user's starred messages must be read`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (message1Id, message2Id) = (1..3).map { Messages.message(user1Id, chatId) }
            listOf(message1Id, message2Id).forEach { Stargazers.create(user1Id, it) }
            assertEquals(listOf(message1Id, message2Id), readStars(user1Id).edges.map { it.node.messageId })
        }

        @Test
        fun `Messages must be paginated`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageIdList = (1..10).map {
                Messages.message(adminId, chatId).also { Stargazers.create(adminId, it) }
            }
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, after = messageIdList[index])
            val actual = readStars(adminId, pagination).edges.map { it.node.messageId }
            assertEquals(messageIdList.slice(index + 1..index + first), actual)
        }
    }

    @Nested
    inner class ReadOnlineStatus {
        @Test
        fun `The online status must be read`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertEquals(Users.readOnlineStatus(userId), readOnlineStatus(userId))
        }

        @Test
        fun `Reading the online status of a nonexistent user must fail`(): Unit =
            assertTrue(readOnlineStatus(userId = -1) is InvalidUserId)
    }

    @Nested
    inner class ReadAccount {
        @Test
        fun `The user's account info must be returned`() {
            val user = createVerifiedUsers(1).first().info
            assertEquals(user, readAccount(user.id))
        }
    }

    private data class ReadableChats(val adminId: Int, val chatIdList: LinkedHashSet<Int>)

    @Nested
    inner class ReadChats {
        @Test
        fun `Private chats deleted by the user must be retrieved only for the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(readChats(user1Id).edges.isEmpty())
            assertFalse(readChats(user2Id).edges.isEmpty())
        }

        @Test
        fun `Chats must be paginated`() {
            val (adminId, chatIdList) = createReadableChats()
            val index = 5
            val first = 3
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = readChats(adminId, pagination).edges.map { it.node.id }.toLinkedHashSet()
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `Messages must be paginated`(): Unit = testMessagesPagination(MessagesOperationName.READ_CHATS)

        @Test
        fun `Group chat users must be paginated`(): Unit =
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHATS)
    }

    private fun createReadableChats(count: Int = 10): ReadableChats {
        val adminId = createVerifiedUsers(1).first().info.id
        val chatIdList = (1..count).map { GroupChats.create(listOf(adminId)) }.toLinkedHashSet()
        return ReadableChats(adminId, chatIdList)
    }

    @Nested
    inner class BuildChatsConnection {

        @Test
        fun `When requesting items after the start cursor, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {
            val (adminId, chatIdList) = createReadableChats()
            val pagination = ForwardPagination(after = chatIdList.first())
            val (hasNextPage, hasPreviousPage) = readChats(adminId, pagination).pageInfo
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (adminId, chatIdList) = createReadableChats()
            assertEquals(chatIdList, readChats(adminId).edges.map { it.node.id }.toLinkedHashSet())
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, chatIdList) = createReadableChats()
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = readChats(adminId, pagination).edges.map { it.node.id }.toLinkedHashSet()
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, chatIdList) = createReadableChats()
            val first = 3
            val actual = readChats(adminId, ForwardPagination(first)).edges.map { it.node.id }
            assertEquals(chatIdList.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, chatIdList) = createReadableChats()
            val index = 5
            val pagination = ForwardPagination(after = chatIdList.elementAt(index))
            assertEquals(chatIdList.drop(index + 1), readChats(adminId, pagination).edges.map { it.node.id })
        }

        @Test
        fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {
            val (adminId, chatIdList) = createReadableChats()
            val pagination = ForwardPagination(after = chatIdList.last())
            val (edges, pageInfo) = readChats(adminId, pagination)
            assertEquals(0, edges.size)
            assertFalse(pageInfo.hasNextPage)
            assertTrue(pageInfo.hasPreviousPage)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (adminId, chatIdList) = createReadableChats()
            GroupChatUsers.removeUsers(chatIdList.elementAt(3), adminId)
            val pagination = ForwardPagination(first = 3, after = chatIdList.elementAt(1))
            val expected = listOf(chatIdList.elementAt(2), chatIdList.elementAt(4), chatIdList.elementAt(5))
            val actual = readChats(adminId, pagination).edges.map { it.node.id }
            assertEquals(expected, actual)
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (adminId, chatIdList) = createReadableChats()
            val index = 5
            val chatId = chatIdList.elementAt(index)
            GroupChatUsers.removeUsers(chatId, adminId)
            val actual = readChats(adminId, ForwardPagination(after = chatId)).edges.map { it.node.id }
            assertEquals(chatIdList.drop(index + 1), actual)
        }

        @Test
        fun `Retrieving the first of many items must cause the page info to state there are only items after it`() {
            val (adminId) = createReadableChats()
            val pageInfo = readChats(adminId, ForwardPagination(first = 1)).pageInfo
            assertTrue(pageInfo.hasNextPage)
        }

        @Test
        fun `Retrieving the last of many items must cause the page info to state there are only items before it`() {
            val (adminId, chatIdList) = createReadableChats()
            val pagination = ForwardPagination(after = chatIdList.last())
            assertTrue(readChats(adminId, pagination).pageInfo.hasPreviousPage)
        }

        @Test
        fun `If there are no items, the page info must indicate such`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            assertEquals(expected, readChats(adminId).pageInfo)
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val (adminId, chatIdList) = createReadableChats(count = 1)
            val expected = PageInfo(
                hasNextPage = false,
                hasPreviousPage = false,
                startCursor = chatIdList.first(),
                endCursor = chatIdList.first(),
            )
            assertEquals(expected, readChats(adminId).pageInfo)
        }

        @Test
        fun `When requesting zero items sans cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId) = createReadableChats()
            val (hasNextPage, hasPreviousPage) = readChats(adminId, ForwardPagination(first = 0)).pageInfo
            assertTrue(hasNextPage)
            assertFalse(hasPreviousPage)
        }

        @Test
        fun `When requesting zero items after the end cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId, chatIdList) = createReadableChats()
            val pagination = ForwardPagination(first = 0, after = chatIdList.last())
            val (hasNextPage, hasPreviousPage) = readChats(adminId, pagination).pageInfo
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Given items 1-10, when requesting zero items after item 5, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId, chatIdList) = createReadableChats()
            val pagination = ForwardPagination(first = 0, after = chatIdList.elementAt(4))
            val (hasNextPage, hasPreviousPage) = readChats(adminId, pagination).pageInfo
            assertTrue(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `The first and last cursors must be the first and last items respectively`() {
            val (adminId, chatIdList) = createReadableChats()
            val (_, _, startCursor, endCursor) = readChats(adminId).pageInfo
            assertEquals(chatIdList.first(), startCursor)
            assertEquals(chatIdList.last(), endCursor)
        }
    }

    @Nested
    inner class ReadChat {
        @Test
        fun `The private chat the user just deleted must be read`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(PrivateChats.read(chatId, user1Id), readChat(chatId, userId = user1Id))
        }

        @Test
        fun `Reading a public chat mustn't require an access token`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(GroupChats.readChat(chatId), readChat(chatId))
        }

        @Test
        fun `When a user reads a public chat, the chat must be represented the way they see it`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val chat = readChat(chatId, userId = adminId) as GroupChat
            assertEquals(listOf(true), chat.messages.edges.map { it.node.hasStar })
        }

        @Test
        fun `Requesting a chat using an invalid ID must return an error`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(readChat(id = 1, userId = userId) is InvalidChatId)
        }

        @Test
        fun `Messages must be paginated`(): Unit = testMessagesPagination(MessagesOperationName.READ_CHAT)

        @Test
        fun `Group chat users must be paginated`(): Unit =
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHAT)
    }

    @Nested
    inner class ReadContacts {
        @Test
        fun `Contacts must be read`() {
            val (owner, contact1, contact2) = createVerifiedUsers(3).map { it.info }
            Contacts.createAll(owner.id, setOf(contact1.id, contact2.id))
            assertEquals(listOf(contact1, contact2), readContacts(owner.id).edges.map { it.node })
        }

        @Test
        fun `Contacts must be paginated`(): Unit = testContactsPagination(ContactsOperationName.READ_CONTACTS)
    }

    @Nested
    inner class RefreshTokenSet {
        @Test
        fun `A refresh token must issue a new token set`() {
            val userId = createVerifiedUsers(1).first().info.id
            val refreshToken = buildTokenSet(userId).refreshToken
            refreshTokenSet(refreshToken)
        }

        @Test
        fun `An invalid refresh token must return an authorization error`() {
            val variables = mapOf("refreshToken" to "invalid token")
            val response = executeGraphQlViaHttp(REFRESH_TOKEN_SET_QUERY, variables)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class RequestTokenSet {
        @Test
        fun `The access token must work`() {
            val login = createVerifiedUsers(1).first().login
            val tokenSet = requestTokenSet(login) as TokenSet
            val response = executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = tokenSet.accessToken)
            assertNotEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `A nonexistent user must cause an exception to be thrown`() {
            val login = Login(Username("u"), Password("p"))
            assertTrue(requestTokenSet(login) is NonexistentUser)
        }

        @Test
        fun `A user who hasn't verified their email must cause an exception to be thrown`() {
            val login = Login(Username("u"), Password("p"))
            Users.create(AccountInput(login.username, login.password, "username@example.com"))
            assertTrue(requestTokenSet(login) is UnverifiedEmailAddress)
        }

        @Test
        fun `An incorrect password must cause an exception to be thrown`() {
            val login = createVerifiedUsers(1).first().login
            val invalidLogin = login.copy(password = Password("incorrect password"))
            assertTrue(requestTokenSet(invalidLogin) is IncorrectPassword)
        }
    }

    @Nested
    inner class SearchChatMessages {
        @Test
        fun `Messages must be searched case-insensitively`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId, MessageText("Hey!"))
            Messages.create(user2Id, chatId, MessageText(":) hey"))
            Messages.create(user1Id, chatId, MessageText("How are you?"))
            val edges = searchChatMessages(chatId, "hey", userId = user1Id) as MessageEdges
            assertEquals(Messages.readPrivateChat(user1Id, chatId).toList().dropLast(1), edges.edges)
        }

        @Test
        fun `Searching in a non-public chat the user isn't in must return an error`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = PrivateChats.create(user2Id, user3Id)
            assertTrue(searchChatMessages(chatId, "query", userId = user1Id) is InvalidChatId)
        }

        @Test
        fun `A public chat must be searchable without an account`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val text = "text"
            val messageId = Messages.message(adminId, chatId, MessageText(text))
            val edges = searchChatMessages(chatId, text) as MessageEdges
            assertEquals(listOf(messageId), edges.edges.map { it.node.messageId })
        }

        @Test
        fun `When a user searches a public chat, it must be returned as it's seen by the user`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val text = "t"
            val messageId = Messages.message(adminId, chatId, MessageText(text))
            Stargazers.create(adminId, messageId)
            val edges = searchChatMessages(chatId, text, userId = adminId) as MessageEdges
            assertEquals(listOf(true), edges.edges.map { it.node.hasStar })
        }

        @Test
        fun `Messages must be paginated`(): Unit = testMessagesPagination(MessagesOperationName.SEARCH_CHAT_MESSAGES)
    }

    @Nested
    inner class SearchChats {
        @Test
        fun `Searching a private chat the user deleted mustn't include the chat in the search results`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val chatId = PrivateChats.create(user1.id, user2.id)
            PrivateChatDeletions.create(chatId, user1.id)
            assertTrue(searchChats(user1.id, user2.username.value).edges.isEmpty())
        }

        @Test
        fun `Chats must be paginated`() {
            val (adminId, chatIdList) = createSearchableChats()
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = searchChats(adminId, query = "", pagination).edges.map { it.node.id }.toLinkedHashSet()
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `Messages must be paginated`(): Unit = testMessagesPagination(MessagesOperationName.SEARCH_CHATS)

        @Test
        fun `Group chat users must be paginated`(): Unit =
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_CHATS)
    }

    @Nested
    inner class SearchContacts {
        @Test
        fun `Contacts must be searched case-insensitively`() {
            val accounts = listOf(
                AccountInput(Username("john_doe"), Password("p"), emailAddress = "john.doe@example.com"),
                AccountInput(Username("john_roger"), Password("p"), emailAddress = "john.roger@example.com"),
                AccountInput(Username("nick_bostrom"), Password("p"), emailAddress = "nick.bostrom@example.com"),
                AccountInput(Username("iron_man"), Password("p"), emailAddress = "roger@example.com", Name("John")),
            ).map {
                Users.create(it)
                it.toAccount()
            }
            val userId = createVerifiedUsers(1).first().info.id
            Contacts.createAll(userId, accounts.map { it.id }.toSet())
            val testContacts = { query: String, accountList: List<Account> ->
                assertEquals(accountList, searchContacts(userId, query).edges.map { it.node })
            }
            testContacts("john", listOf(accounts[0], accounts[1], accounts[3]))
            testContacts("bost", listOf(accounts[2]))
            testContacts("Roger", listOf(accounts[1], accounts[3]))
        }

        @Test
        fun `Contacts must be paginated`(): Unit = testContactsPagination(ContactsOperationName.SEARCH_CONTACTS)
    }

    @Nested
    inner class SearchMessages {
        @Test
        fun `Searching for a message sent before the private chat was deleted mustn't be found`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val text = "text"
            Messages.message(user1Id, chatId, MessageText(text))
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(searchMessages(user1Id, text).edges.isEmpty())
        }

        @Test
        fun `Group chat users must be paginated`(): Unit =
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)

        @Test
        fun `Group chat messages must be paginated`(): Unit =
            testMessagesPagination(MessagesOperationName.SEARCH_MESSAGES)

        @Test
        fun `Chats must be paginated`() {
            val (adminId, chatIdList) = createSearchableChats()
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = searchMessages(adminId, "", pagination).edges.map { it.node.chat.id }.toLinkedHashSet()
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }
    }

    private data class SearchableChats(val adminId: Int, val chatIdList: LinkedHashSet<Int>)

    private fun createSearchableChats(count: Int = 10): SearchableChats {
        val adminId = createVerifiedUsers(1).first().info.id
        val chatIdList = (1..count)
            .map {
                val chatId = GroupChats.create(listOf(adminId))
                Messages.create(adminId, chatId)
                chatId
            }
            .toLinkedHashSet()
        return SearchableChats(adminId, chatIdList)
    }

    @Nested
    inner class BuildChatMessagesConnection {
        @Test
        fun `Every item must be retrieved if neither cursor nor limit get supplied`() {
            val (adminId, chatIdList) = createSearchableChats()
            val actual = searchMessages(adminId, query = "").edges.map { it.node.chat.id }.toLinkedHashSet()
            assertEquals(chatIdList, actual)
        }

        @Test
        fun `The number of items specified by the limit must be returned from after the cursor`() {
            val (adminId, chatIdList) = createSearchableChats()
            val first = 3
            val index = 5
            val pagination = ForwardPagination(first, after = chatIdList.elementAt(index))
            val actual = searchMessages(adminId, query = "", pagination).edges.map { it.node.chat.id }.toLinkedHashSet()
            assertEquals(chatIdList.slice(index + 1..index + first), actual)
        }

        @Test
        fun `When requesting items after the start cursor, 'hasNextPage' must be 'false', and 'hasPreviousPage' must be 'true'`() {
            val (adminId, chatIdList) = createSearchableChats()
            val pagination = ForwardPagination(after = chatIdList.first())
            val (hasNextPage, hasPreviousPage) = searchMessages(adminId, query = "", pagination).pageInfo
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `The number of items specified by the limit from the first item must be retrieved when there's no cursor`() {
            val (adminId, chatIdList) = createSearchableChats()
            val first = 3
            val actual = searchMessages(adminId, query = "", ForwardPagination(first)).edges.map { it.node.chat.id }
            assertEquals(chatIdList.take(first), actual)
        }

        @Test
        fun `Every item after the cursor must be retrieved when there's no limit`() {
            val (adminId, chatIdList) = createSearchableChats()
            val index = 5
            val pagination = ForwardPagination(after = chatIdList.elementAt(index))
            val actual = searchMessages(adminId, query = "", pagination).edges.map { it.node.chat.id }
            assertEquals(chatIdList.drop(index + 1), actual)
        }

        @Test
        fun `Zero items must be retrieved along with the correct 'hasNextPage' and 'hasPreviousPage' when using the last item's cursor`() {
            val (adminId, chatIdList) = createSearchableChats()
            val pagination = ForwardPagination(after = chatIdList.last())
            val (edges, pageInfo) = searchMessages(adminId, query = "", pagination)
            assertEquals(0, edges.size)
            assertFalse(pageInfo.hasNextPage)
            assertTrue(pageInfo.hasPreviousPage)
        }

        @Test
        fun `Given items 1-10 where item 4 has been deleted, when requesting the first three items after item 2, then items 3, 5, and 6 must be retrieved`() {
            val (adminId, chatIdList) = createSearchableChats()
            GroupChatUsers.removeUsers(chatIdList.elementAt(3), adminId)
            val expected = listOf(chatIdList.elementAt(2), chatIdList.elementAt(4), chatIdList.elementAt(5))
            val pagination = ForwardPagination(first = 3, after = chatIdList.elementAt(1))
            val actual = searchMessages(adminId, query = "", pagination).edges.map { it.node.chat.id }
            assertEquals(expected, actual)
        }

        @Test
        fun `Using a deleted item's cursor must cause pagination to work as if the item still exists`() {
            val (adminId, chatIdList) = createSearchableChats()
            val index = 5
            val chatId = chatIdList.elementAt(index)
            GroupChatUsers.removeUsers(chatId, adminId)
            val edges = searchMessages(adminId, query = "", ForwardPagination(after = chatId)).edges
            assertEquals(chatIdList.drop(index + 1), edges.map { it.node.chat.id })
        }

        @Test
        fun `Retrieving the first of many items must cause the page info to state there are only items after it`() {
            val (adminId) = createSearchableChats()
            val pageInfo = searchMessages(adminId, query = "", ForwardPagination(first = 1)).pageInfo
            assertTrue(pageInfo.hasNextPage)
        }

        @Test
        fun `Retrieving the last of many items must cause the page info to state there are only items before it`() {
            val (adminId, chatIdList) = createSearchableChats()
            val pagination = ForwardPagination(first = 1, after = chatIdList.elementAt(5))
            val pageInfo = searchMessages(adminId, query = "", pagination).pageInfo
            assertTrue(pageInfo.hasPreviousPage)
        }

        @Test
        fun `If there are no items, the page info must indicate such`() {
            val (adminId) = createSearchableChats(count = 0)
            val expected = PageInfo(hasNextPage = false, hasPreviousPage = false, startCursor = null, endCursor = null)
            assertEquals(expected, searchMessages(adminId, query = "").pageInfo)
        }

        @Test
        fun `If there's one item, the page info must indicate such`() {
            val (adminId, chatIdList) = createSearchableChats(count = 1)
            val expected = PageInfo(
                hasNextPage = false,
                hasPreviousPage = false,
                startCursor = chatIdList.first(),
                endCursor = chatIdList.first(),
            )
            assertEquals(expected, searchMessages(adminId, query = "").pageInfo)
        }

        @Test
        fun `When requesting zero items sans cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId) = createSearchableChats()
            val (hasNextPage, hasPreviousPage) =
                searchMessages(adminId, query = "", ForwardPagination(first = 0)).pageInfo
            assertTrue(hasNextPage)
            assertFalse(hasPreviousPage)
        }

        @Test
        fun `When requesting zero items after the end cursor, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId, chatIdList) = createSearchableChats()
            val pagination = ForwardPagination(first = 0, after = chatIdList.last())
            val (hasNextPage, hasPreviousPage) = searchMessages(adminId, query = "", pagination).pageInfo
            assertFalse(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `Given items 1-10, when requesting zero items after item 5, the 'hasNextPage' and 'hasPreviousPage' must indicate such`() {
            val (adminId, chatIdList) = createSearchableChats()
            val pagination = ForwardPagination(first = 0, after = chatIdList.elementAt(4))
            val (hasNextPage, hasPreviousPage) = searchMessages(adminId, query = "", pagination).pageInfo
            assertTrue(hasNextPage)
            assertTrue(hasPreviousPage)
        }

        @Test
        fun `The first and last cursors must be the first and last items respectively`() {
            val (adminId, chatIdList) = createSearchableChats()
            val (_, _, startCursor, endCursor) = searchMessages(adminId, query = "").pageInfo
            assertEquals(chatIdList.first(), startCursor)
            assertEquals(chatIdList.last(), endCursor)
        }
    }

    @Nested
    inner class SearchUsers {
        @Test
        fun `Users must be searched`() {
            val accounts = listOf(
                AccountInput(Username("iron_man"), Password("p"), "tony@example.com"),
                AccountInput(Username("iron_fist"), Password("p"), "iron_fist@example.com"),
                AccountInput(Username("hulk"), Password("p"), "bruce@example.com"),
            ).map {
                Users.create(it)
                it.toAccount()
            }
            assertEquals(accounts.dropLast(1), searchUsers("iron").edges.map { it.node })
        }

        private fun createAccounts(): Set<AccountInput> {
            val accounts = setOf(
                AccountInput(Username("iron_man"), Password("p"), "iron.man@example.com"),
                AccountInput(Username("tony_hawk"), Password("p"), "tony.hawk@example.com"),
                AccountInput(Username("lol"), Password("p"), "iron.fist@example.com"),
                AccountInput(Username("another_one"), Password("p"), "another_one@example.com"),
                AccountInput(Username("jo_mama"), Password("p"), "mama@example.com", firstName = Name("Iron")),
                AccountInput(Username("nope"), Password("p"), "nope@example.com", lastName = Name("Irony")),
                AccountInput(Username("black_widow"), Password("p"), "black.widow@example.com"),
                AccountInput(Username("iron_spider"), Password("p"), "iron.spider@example.com"),
            )
            accounts.forEach(Users::create)
            return accounts
        }

        @Test
        fun `Users must be paginated`() {
            val infoCursors = createAccounts()
                .zip(Users.read())
                .map { (newAccount, cursor) -> AccountEdge(newAccount.toAccount(), cursor) }
            val searchedUsers = listOf(infoCursors[0], infoCursors[2], infoCursors[4], infoCursors[5], infoCursors[7])
            val first = 3
            val index = 0
            val users = searchUsers("iron", ForwardPagination(first, infoCursors[index].cursor)).edges
            assertEquals(searchedUsers.subList(index + 1, index + 1 + first), users)
        }
    }
}

/** The name of a GraphQL operation which is concerned with the pagination of blocked users. */
private enum class BlockedUsersOperationName {
    /** Represents `Query.readBlockedUsers`. */
    READ_BLOCKED_USERS,

    /** Represents `Query.searchBlockedUsers`. */
    SEARCH_BLOCKED_USERS,
}

private fun testBlockedUsersPagination(operation: BlockedUsersOperationName) {
    val blockerId = createVerifiedUsers(1).first().info.id
    val userIdList = createVerifiedUsers(10).map { it.info.id }
    userIdList.forEach { BlockedUsers.create(blockerId, it) }
    val index = 5
    val first = 3
    val expected = userIdList.subList(index + 1, index + 1 + first)
    val actual = when (operation) {
        BlockedUsersOperationName.READ_BLOCKED_USERS -> {
            val cursor = BlockedUsers.read(blockerId).edges[index].cursor
            readBlockedUsers(blockerId, ForwardPagination(first, cursor))
        }
        BlockedUsersOperationName.SEARCH_BLOCKED_USERS -> {
            val cursor = BlockedUsers.search(blockerId, query = "").edges[index].cursor
            searchBlockedUsers(blockerId, query = "", ForwardPagination(first, cursor))
        }
    }
    assertEquals(expected, actual.edges.map { it.node.id })
}

/** The name of a GraphQL operation which is concerned with the pagination of messages. */
private enum class MessagesOperationName {
    /** Represents `Query.searchChatMessages`. */
    SEARCH_CHAT_MESSAGES,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS,
}

/** Asserts that the [operation] paginates correctly. */
private fun testMessagesPagination(operation: MessagesOperationName) {
    val adminId = createVerifiedUsers(1).first().info.id
    val chatId = GroupChats.create(listOf(adminId))
    val text = MessageText("t")
    val messageIdList = (1..10).map { Messages.message(adminId, chatId, text) }
    val last = 4
    val cursorIndex = 3
    val pagination = BackwardPagination(last, before = messageIdList[cursorIndex])
    val messages = when (operation) {
        MessagesOperationName.SEARCH_CHAT_MESSAGES -> {
            val messages = searchChatMessages(chatId, text.value, pagination, adminId) as MessageEdges
            messages.edges
        }
        MessagesOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text.value, chatMessagesPagination = pagination).edges.flatMap { it.node.messages }
        MessagesOperationName.READ_CHATS ->
            readChats(adminId, groupChatMessagesPagination = pagination).edges[0].node.messages.edges
        MessagesOperationName.READ_CHAT -> {
            val chat = readChat(chatId, groupChatMessagesPagination = pagination, userId = adminId) as GroupChat
            chat.messages.edges
        }
        MessagesOperationName.SEARCH_CHATS -> {
            val title = GroupChats.readChat(chatId, userId = adminId).title.value
            searchChats(adminId, title, groupChatMessagesPagination = pagination).edges[0].node.messages.edges
        }
    }
    assertEquals(messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last), messages.map { it.cursor })
}

/** The name of a GraphQL operation which is concerned with the pagination of contacts. */
private enum class ContactsOperationName {
    /** Represents `Query.readContacts`. */
    READ_CONTACTS,

    /** Represents `Query.searchContacts`. */
    SEARCH_CONTACTS,
}

private fun testContactsPagination(operation: ContactsOperationName) {
    val ownerId = createVerifiedUsers(1).first().info.id
    val userList = createVerifiedUsers(10).map { it.info }
    Contacts.createAll(ownerId, userList.map { it.id }.toSet())
    val index = 5
    val cursor = Contacts.read(ownerId).edges[index].cursor
    val first = 3
    val contacts = when (operation) {
        ContactsOperationName.READ_CONTACTS -> readContacts(ownerId, ForwardPagination(first, cursor))
        ContactsOperationName.SEARCH_CONTACTS ->
            searchContacts(ownerId, query = "username", ForwardPagination(first, cursor))
    }.edges.map { it.node }
    val expected = userList.subList(index + 1, index + 1 + first)
    assertEquals(expected, contacts)
}

/** The name of a GraphQL operation which is concerned with the pagination of accounts. */
private enum class GroupChatUsersOperationName {
    /** Represents `Query.readChat`. */
    READ_CHAT,

    /** Represents `Query.readChats`. */
    READ_CHATS,

    /** Represents `Query.searchChats`. */
    SEARCH_CHATS,

    /** Represents `Query.searchMessages`. */
    SEARCH_MESSAGES,
}

private fun testGroupChatUsersPagination(operationName: GroupChatUsersOperationName) {
    val adminId = createVerifiedUsers(1).first().info.id
    val users = createVerifiedUsers(10)
    val userIdList = users.map { it.info.id }
    val chatId = GroupChats.create(listOf(adminId), userIdList)
    val text = "text"
    Messages.create(adminId, chatId, MessageText(text))
    val first = 3
    val userCursors = GroupChatUsers.read()
    val index = 5
    val pagination = ForwardPagination(first, after = userCursors.elementAt(index))
    val chat = when (operationName) {
        GroupChatUsersOperationName.READ_CHAT ->
            readChat(chatId, usersPagination = pagination, userId = adminId) as GroupChat
        GroupChatUsersOperationName.READ_CHATS -> readChats(adminId, usersPagination = pagination).edges[0].node
        GroupChatUsersOperationName.SEARCH_CHATS -> {
            val title = GroupChats.readChat(chatId, userId = adminId).title.value
            searchChats(adminId, title, usersPagination = pagination).edges[0].node
        }
        GroupChatUsersOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text, usersPagination = pagination).edges[0].node.chat
    } as GroupChat
    val expected = userCursors.toList().subList(index + 1, index + 1 + first).map { it }
    assertEquals(expected, chat.users.edges.map { it.cursor })
}
