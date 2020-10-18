package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.buildTokenSet
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.testingObjectMapper
import io.ktor.http.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.*

const val SEARCH_PUBLIC_CHATS_QUERY = """
    query SearchPublicChats(
        ${"$"}query: String!
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchPublicChats(query: ${"$"}query) {
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchPublicChats(
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_PUBLIC_CHATS_QUERY,
    mapOf(
        "query" to query,
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to messagesPagination?.last,
        "groupChat_messages_before" to messagesPagination?.before?.toString()
    )
)

fun searchPublicChats(
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null
): List<GroupChat> {
    val data =
        operateSearchPublicChats(query, usersPagination, messagesPagination).data!!["searchPublicChats"] as List<*>
    return testingObjectMapper.convertValue(data)
}

const val READ_GROUP_CHAT_QUERY = """
    query ReadGroupChat(
        ${"$"}inviteCode: Uuid!
        ${"$"}groupChatInfo_users_first: Int
        ${"$"}groupChatInfo_users_after: Cursor
    ) {
        readGroupChat(inviteCode: ${"$"}inviteCode) {
            $GROUP_CHAT_INFO_FRAGMENT
        }
    }
"""

private fun operateReadGroupChat(inviteCode: UUID, usersPagination: ForwardPagination? = null): GraphQlResponse =
    executeGraphQlViaEngine(
        READ_GROUP_CHAT_QUERY,
        mapOf(
            "inviteCode" to inviteCode.toString(),
            "groupChatInfo_users_first" to usersPagination?.first,
            "groupChatInfo_users_after" to usersPagination?.after?.toString()
        )
    )

fun readGroupChat(inviteCode: UUID, usersPagination: ForwardPagination? = null): GroupChatInfo {
    val data = operateReadGroupChat(inviteCode, usersPagination).data!!["readGroupChat"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

fun errReadGroupChat(inviteCode: UUID, usersPagination: ForwardPagination? = null): String =
    operateReadGroupChat(inviteCode, usersPagination).errors!![0].message

const val READ_STARS_QUERY = """
    query ReadStars {
        readStars {
            $STARRED_MESSAGE_FRAGMENT
        }
    }
"""

private fun operateReadStars(userId: Int): GraphQlResponse = executeGraphQlViaEngine(READ_STARS_QUERY, userId = userId)

fun readStars(userId: Int): List<StarredMessage> {
    val data = operateReadStars(userId).data!!["readStars"] as List<*>
    return testingObjectMapper.convertValue(data)
}

const val READ_ONLINE_STATUSES_QUERY = """
    query ReadOnlineStatuses {
        readOnlineStatuses {
            $ONLINE_STATUS_FRAGMENT
        }
    }
"""

private fun operateReadOnlineStatuses(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(READ_ONLINE_STATUSES_QUERY, userId = userId)

fun readOnlineStatuses(userId: Int): List<OnlineStatus> {
    val data = operateReadOnlineStatuses(userId).data!!["readOnlineStatuses"] as List<*>
    return testingObjectMapper.convertValue(data)
}

const val CAN_DELETE_ACCOUNT_QUERY = """
    query CanDeleteAccount {
        canDeleteAccount
    }
"""

private fun operateCanDeleteAccount(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(CAN_DELETE_ACCOUNT_QUERY, userId = userId)

fun canDeleteAccount(userId: Int): Boolean = operateCanDeleteAccount(userId).data!!["canDeleteAccount"] as Boolean

const val IS_EMAIL_ADDRESS_TAKEN_QUERY = """
    query IsEmailAddressTaken(${"$"}emailAddress: String!) {
        isEmailAddressTaken(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateIsEmailAddressTaken(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(IS_EMAIL_ADDRESS_TAKEN_QUERY, mapOf("emailAddress" to emailAddress))

fun isEmailAddressTaken(emailAddress: String): Boolean =
    operateIsEmailAddressTaken(emailAddress).data!!["isEmailAddressTaken"] as Boolean

const val IS_USERNAME_TAKEN_QUERY = """
    query IsUsernameTaken(${"$"}username: Username!) {
        isUsernameTaken(username: ${"$"}username)
    }
"""

private fun operateIsUsernameTaken(username: Username): GraphQlResponse =
    executeGraphQlViaEngine(IS_USERNAME_TAKEN_QUERY, mapOf("username" to username))

fun isUsernameTaken(username: Username): Boolean = operateIsUsernameTaken(username).data!!["isUsernameTaken"] as Boolean

const val READ_ACCOUNT_QUERY = """
    query ReadAccount {
        readAccount {
            $ACCOUNT_FRAGMENT
        }
    }
"""

private fun operateReadAccount(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(READ_ACCOUNT_QUERY, userId = userId)

fun readAccount(userId: Int): Account {
    val data = operateReadAccount(userId).data!!["readAccount"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

const val READ_CHATS_QUERY = """
    query ReadChats(
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        readChats {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChats(
    userId: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    READ_CHATS_QUERY,
    mapOf(
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId
)

fun readChats(
    userId: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateReadChats(
        userId,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["readChats"] as List<*>
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
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateReadChat(
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
    userId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    READ_CHAT_QUERY,
    mapOf(
        "id" to id,
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId
)

fun readChat(
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
    userId: Int? = null
): Chat {
    val data = operateReadChat(
        id,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination,
        userId
    ).data!!["readChat"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

fun errReadChat(
    id: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
    userId: Int? = null
): String = operateReadChat(
    id,
    privateChatMessagesPagination,
    usersPagination,
    groupChatMessagesPagination,
    userId
).errors!![0].message

const val READ_CONTACTS_QUERY = """
    query ReadContacts(${"$"}first: Int, ${"$"}after: Cursor) {
        readContacts(first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateReadContacts(userId: Int, pagination: ForwardPagination? = null): GraphQlResponse =
    executeGraphQlViaEngine(
        READ_CONTACTS_QUERY,
        mapOf("first" to pagination?.first, "after" to pagination?.after?.toString()),
        userId
    )

fun readContacts(userId: Int, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateReadContacts(userId, pagination).data!!["readContacts"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

const val REFRESH_TOKEN_SET_QUERY = """
    query RefreshTokenSet(${"$"}refreshToken: ID!) {
        refreshTokenSet(refreshToken: ${"$"}refreshToken) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRefreshTokenSet(refreshToken: String): GraphQlResponse =
    executeGraphQlViaEngine(REFRESH_TOKEN_SET_QUERY, mapOf("refreshToken" to refreshToken))

fun refreshTokenSet(refreshToken: String): TokenSet {
    val data = operateRefreshTokenSet(refreshToken).data!!["refreshTokenSet"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

const val REQUEST_TOKEN_SET_QUERY = """
    query RequestTokenSet(${"$"}login: Login!) {
        requestTokenSet(login: ${"$"}login) {
            $TOKEN_SET_FRAGMENT
        }
    }
"""

private fun operateRequestTokenSet(login: Login): GraphQlResponse =
    executeGraphQlViaEngine(REQUEST_TOKEN_SET_QUERY, mapOf("login" to login))

fun requestTokenSet(login: Login): TokenSet {
    val data = operateRequestTokenSet(login).data!!["requestTokenSet"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

fun errRequestTokenSet(login: Login): String = operateRequestTokenSet(login).errors!![0].message

const val SEARCH_CHAT_MESSAGES_QUERY = """
    query SearchChatMessages(${"$"}chatId: Int!, ${"$"}query: String!, ${"$"}last: Int, ${"$"}before: Cursor) {
        searchChatMessages(chatId: ${"$"}chatId, query: ${"$"}query, last: ${"$"}last, before: ${"$"}before) {
            $MESSAGE_EDGE_FRAGMENT
        }
    }
"""

private fun operateSearchChatMessages(
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null,
    userId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_CHAT_MESSAGES_QUERY,
    mapOf(
        "chatId" to chatId,
        "query" to query,
        "last" to pagination?.last,
        "before" to pagination?.before?.toString()
    ),
    userId
)

fun searchChatMessages(
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null,
    userId: Int? = null
): List<MessageEdge> {
    val data = operateSearchChatMessages(chatId, query, pagination, userId).data!!["searchChatMessages"] as List<*>
    return testingObjectMapper.convertValue(data)
}

fun errSearchChatMessages(
    chatId: Int,
    query: String,
    pagination: BackwardPagination? = null,
    userId: Int? = null
): String = operateSearchChatMessages(chatId, query, pagination, userId).errors!![0].message

const val SEARCH_CHATS_QUERY = """
    query SearchChats(
        ${"$"}query: String!
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchChats(query: ${"$"}query) {
            $PRIVATE_CHAT_FRAGMENT
            $GROUP_CHAT_FRAGMENT
        }
    }
"""

private fun operateSearchChats(
    userId: Int,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_CHATS_QUERY,
    mapOf(
        "query" to query,
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId = userId
)

fun searchChats(
    userId: Int,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<Chat> {
    val chats = operateSearchChats(
        userId,
        query,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["searchChats"] as List<*>
    return testingObjectMapper.convertValue(chats)
}

const val SEARCH_CONTACTS_QUERY = """
    query SearchContacts(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchContacts(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchContacts(
    userId: Int,
    query: String,
    pagination: ForwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_CONTACTS_QUERY,
    mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString()),
    userId
)

fun searchContacts(userId: Int, query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateSearchContacts(userId, query, pagination).data!!["searchContacts"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

const val SEARCH_MESSAGES_QUERY = """
    query SearchMessages(
        ${"$"}query: String!
        ${"$"}chatMessages_messages_last: Int
        ${"$"}chatMessages_messages_before: Cursor
        ${"$"}privateChat_messages_last: Int
        ${"$"}privateChat_messages_before: Cursor
        ${"$"}groupChat_users_first: Int
        ${"$"}groupChat_users_after: Cursor
        ${"$"}groupChat_messages_last: Int
        ${"$"}groupChat_messages_before: Cursor
    ) {
        searchMessages(query: ${"$"}query) {
            $CHAT_MESSAGES_FRAGMENT
        }
    }
"""

private fun operateSearchMessages(
    userId: Int,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): GraphQlResponse = executeGraphQlViaEngine(
    SEARCH_MESSAGES_QUERY,
    mapOf(
        "query" to query,
        "chatMessages_messages_last" to chatMessagesPagination?.last,
        "chatMessages_messages_before" to chatMessagesPagination?.before?.toString(),
        "privateChat_messages_last" to privateChatMessagesPagination?.last,
        "privateChat_messages_before" to privateChatMessagesPagination?.before?.toString(),
        "groupChat_users_first" to usersPagination?.first,
        "groupChat_users_after" to usersPagination?.after?.toString(),
        "groupChat_messages_last" to groupChatMessagesPagination?.last,
        "groupChat_messages_before" to groupChatMessagesPagination?.before?.toString()
    ),
    userId
)

fun searchMessages(
    userId: Int,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null
): List<ChatMessages> {
    val messages = operateSearchMessages(
        userId,
        query,
        chatMessagesPagination,
        privateChatMessagesPagination,
        usersPagination,
        groupChatMessagesPagination
    ).data!!["searchMessages"] as List<*>
    return testingObjectMapper.convertValue(messages)
}

const val SEARCH_USERS_QUERY = """
    query SearchUsers(${"$"}query: String!, ${"$"}first: Int, ${"$"}after: Cursor) {
        searchUsers(query: ${"$"}query, first: ${"$"}first, after: ${"$"}after) {
            $ACCOUNTS_CONNECTION_FRAGMENT
        }
    }
"""

private fun operateSearchUsers(query: String, pagination: ForwardPagination? = null): GraphQlResponse =
    executeGraphQlViaEngine(
        SEARCH_USERS_QUERY,
        mapOf("query" to query, "first" to pagination?.first, "after" to pagination?.after?.toString())
    )

fun searchUsers(query: String, pagination: ForwardPagination? = null): AccountsConnection {
    val data = operateSearchUsers(query, pagination).data!!["searchUsers"] as Map<*, *>
    return testingObjectMapper.convertValue(data)
}

@ExtendWith(DbExtension::class)
class ChatMessagesDtoTest {
    @Nested
    inner class ReadGroupChat {
        @Test
        fun `The chat's info should be read`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val inviteCode = GroupChats.readInviteCode(chatId)
            assertEquals(GroupChats.readChatInfo(inviteCode), readGroupChat(inviteCode))
        }

        @Test
        fun `Reading a chat using a nonexistent invite code should fail`() {
            assertEquals(InvalidInviteCodeException.message, errReadGroupChat(UUID.randomUUID()))
        }
    }

    /** Data on a group chat having only ever contained an admin. */
    data class AdminMessages(
        /** The ID of the chat's admin. */
        val adminId: Int,
        /** Every message sent has this text. */
        val text: MessageText,
        /** The ten messages the admin sent. */
        val messageIdList: List<Int>
    )

    @Nested
    inner class GetMessages {
        private fun createUtilizedChat(): AdminMessages {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val message = MessageText("t")
            val messageIdList = (1..10).map { Messages.message(adminId, chatId, message) }
            return AdminMessages(adminId, message, messageIdList)
        }

        private fun testPagination(shouldDeleteMessage: Boolean) {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val index = 5
            if (shouldDeleteMessage) Messages.delete(messageIdList[index])
            val last = 3
            val cursors = searchMessages(
                adminId,
                queryText.value,
                chatMessagesPagination = BackwardPagination(last, before = messageIdList[index])
            ).flatMap { it.messages }.map { it.cursor }
            assertEquals(messageIdList.take(index).takeLast(last), cursors)
        }

        @Test
        fun `Messages should paginate using a cursor from a deleted message as if the message still exists`() {
            testPagination(shouldDeleteMessage = true)
        }

        @Test
        fun `Only the messages specified by the cursor and limit should be retrieved`() {
            testPagination(shouldDeleteMessage = false)
        }

        @Test
        fun `If neither cursor nor limit are supplied, every message should be retrieved`() {
            val (adminId, queryText, messageIdList) = createUtilizedChat()
            val cursors = searchMessages(adminId, queryText.value).flatMap { it.messages }.map { it.cursor }
            assertEquals(messageIdList, cursors)
        }
    }
}

@ExtendWith(DbExtension::class)
class QueriesTest {
    @Nested
    inner class SearchPublicChats {
        @Test
        fun `Chats should be case-insensitively queried by their title`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            GroupChats.create(listOf(adminId), title = GroupChatTitle("Kotlin/Native"))
            val chatId = GroupChats
                .create(listOf(adminId), title = GroupChatTitle("Kotlin/JS"), publicity = GroupChatPublicity.PUBLIC)
            GroupChats.create(listOf(adminId), title = GroupChatTitle("Gaming"), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(listOf(chatId), searchPublicChats("kotlin").map { it.id })
        }
    }

    @Nested
    inner class ReadStars {
        @Test
        fun `Only the user's starred messages should be read`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val (message1Id, message2Id) = (1..3).map { Messages.message(user1Id, chatId) }
            listOf(message1Id, message2Id).forEach { Stargazers.create(user1Id, it) }
            assertEquals(listOf(message1Id, message2Id).map { StarredMessage.build(user1Id, it) }, readStars(user1Id))
        }
    }

    @Nested
    inner class ReadOnlineStatuses {
        @Test
        fun `Reading online statuses should only retrieve users the user has in their contacts, or has a chat with`() {
            val (contactOwnerId, contactId, chatSharerId) = createVerifiedUsers(3).map { it.info.id }
            Contacts.create(contactOwnerId, setOf(contactId))
            PrivateChats.create(contactOwnerId, chatSharerId)
            assertEquals(setOf(contactId, chatSharerId), readOnlineStatuses(contactOwnerId).map { it.userId }.toSet())
        }
    }

    @Nested
    inner class CanDeleteAccount {
        @Test
        fun `An account should be deletable if the user is the admin of an otherwise empty chat`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            GroupChats.create(listOf(adminId))
            assertTrue(canDeleteAccount(adminId))
        }

        @Test
        fun `An account shouldn't be deletable if the user is the last admin of a group chat with other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertFalse(canDeleteAccount(adminId))
        }
    }

    @Nested
    inner class IsEmailAddressTaken {
        @Test
        fun `The email shouldn't be taken`() {
            assertFalse(isEmailAddressTaken("username@example.com"))
        }

        @Test
        fun `The email should be taken`() {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            assertTrue(isEmailAddressTaken(address))
        }
    }

    @Nested
    inner class IsUsernameTaken {
        @Test
        fun `The username shouldn't be taken`() {
            assertFalse(isUsernameTaken(Username("u")))
        }

        @Test
        fun `The username should be taken`() {
            val username = createVerifiedUsers(1)[0].info.username
            assertTrue(isUsernameTaken(username))
        }
    }

    @Nested
    inner class ReadAccount {
        @Test
        fun `The user's account info should be returned`() {
            val user = createVerifiedUsers(1)[0].info
            assertEquals(user, readAccount(user.id))
        }
    }

    @Nested
    inner class ReadChats {
        @Test
        fun `Private chats deleted by the user should be retrieved only for the other user`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(readChats(user1Id).isEmpty())
            assertFalse(readChats(user2Id).isEmpty())
        }

        @Test
        fun `Messages should be paginated`() {
            testMessagesPagination(MessagesOperationName.READ_CHATS)
        }

        @Test
        fun `Group chat users should be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHATS)
        }
    }

    @Nested
    inner class ReadChat {
        @Test
        fun `The private chat the user just deleted should be read`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            readChat(chatId, userId = user1Id)
        }

        @Test
        fun `Reading a public chat shouldn't require an access token`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            readChat(chatId)
        }

        @Test
        fun `When a user reads a public chat, the chat should be represented the way they see it`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val chat = readChat(chatId, userId = adminId) as GroupChat
            assertEquals(listOf(true), chat.messages.edges.map { it.node.hasStar })
        }

        @Test
        fun `Requesting a chat using an invalid ID should return an error`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidChatIdException.message, errReadChat(id = 1, userId = userId))
        }

        @Test
        fun `Messages should be paginated`() {
            testMessagesPagination(MessagesOperationName.READ_CHAT)
        }

        @Test
        fun `Group chat users should be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.READ_CHAT)
        }
    }

    @Nested
    inner class ReadContacts {
        @Test
        fun `Contacts should be read`() {
            val (owner, contact1, contact2) = createVerifiedUsers(3).map { it.info }
            Contacts.create(owner.id, setOf(contact1.id, contact2.id))
            assertEquals(listOf(contact1, contact2), readContacts(owner.id).edges.map { it.node })
        }

        @Test
        fun `Contacts should be paginated`() {
            testContactsPagination(ContactsOperationName.READ_CONTACTS)
        }
    }

    @Nested
    inner class RefreshTokenSet {
        @Test
        fun `A refresh token should issue a new token set`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val refreshToken = buildTokenSet(userId).refreshToken
            refreshTokenSet(refreshToken)
        }

        @Test
        fun `An invalid refresh token should return an authorization error`() {
            val variables = mapOf("refreshToken" to "invalid token")
            val response = executeGraphQlViaHttp(REFRESH_TOKEN_SET_QUERY, variables)
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class RequestTokenSet {
        @Test
        fun `The access token should work`() {
            val login = createVerifiedUsers(1)[0].login
            val token = requestTokenSet(login).accessToken
            val response = executeGraphQlViaHttp(READ_ACCOUNT_QUERY, accessToken = token)
            assertNotEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `A nonexistent user should cause an exception to be thrown`() {
            val login = Login(Username("u"), Password("p"))
            assertEquals(NonexistentUserException.message, errRequestTokenSet(login))
        }

        @Test
        fun `A user who hasn't verified their email should cause an exception to be thrown`() {
            val login = Login(Username("u"), Password("p"))
            Users.create(AccountInput(login.username, login.password, "username@example.com"))
            assertEquals(UnverifiedEmailAddressException.message, errRequestTokenSet(login))
        }

        @Test
        fun `An incorrect password should cause an exception to be thrown`() {
            val login = createVerifiedUsers(1)[0].login
            val invalidLogin = login.copy(password = Password("incorrect password"))
            assertEquals(IncorrectPasswordException.message, errRequestTokenSet(invalidLogin))
        }
    }

    @Nested
    inner class SearchChatMessages {
        @Test
        fun `Messages should be searched case-insensitively`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            Messages.create(user1Id, chatId, MessageText("Hey!"))
            Messages.create(user2Id, chatId, MessageText(":) hey"))
            Messages.create(user1Id, chatId, MessageText("How are you?"))
            val messages = searchChatMessages(chatId, "hey", userId = user1Id)
            assertEquals(Messages.readPrivateChat(user1Id, chatId).dropLast(1), messages)
        }

        @Test
        fun `Searching in a non-public chat the user isn't in should return an error`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.info.id }
            val chatId = PrivateChats.create(user2Id, user3Id)
            assertEquals(InvalidChatIdException.message, errSearchChatMessages(chatId, "query", userId = user1Id))
        }

        @Test
        fun `A public chat should be searchable without an account`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            val text = "text"
            val messageId = Messages.message(adminId, chatId, MessageText(text))
            assertEquals(listOf(messageId), searchChatMessages(chatId, text).map { it.node.messageId })
        }

        @Test
        fun `When a user searches a public chat, it should be returned as it's seen by the user`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val text = "t"
            val messageId = Messages.message(adminId, chatId, MessageText(text))
            Stargazers.create(adminId, messageId)
            assertEquals(listOf(true), searchChatMessages(chatId, text, userId = adminId).map { it.node.hasStar })
        }

        @Test
        fun `Messages should be paginated`() {
            testMessagesPagination(MessagesOperationName.SEARCH_CHAT_MESSAGES)
        }
    }

    @Nested
    inner class SearchChats {
        @Test
        fun `Searching a private chat the user deleted shouldn't include the chat in the search results`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val chatId = PrivateChats.create(user1.id, user2.id)
            PrivateChatDeletions.create(chatId, user1.id)
            assertTrue(searchChats(user1.id, user2.username.value).isEmpty())
        }

        @Test
        fun `Messages should be paginated`() {
            testMessagesPagination(MessagesOperationName.SEARCH_CHATS)
        }

        @Test
        fun `Group chat users should be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_CHATS)
        }
    }

    @Nested
    inner class SearchContacts {
        @Test
        fun `Contacts should be searched case-insensitively`() {
            val accounts = listOf(
                AccountInput(Username("john_doe"), Password("p"), emailAddress = "john.doe@example.com"),
                AccountInput(Username("john_roger"), Password("p"), emailAddress = "john.roger@example.com"),
                AccountInput(Username("nick_bostrom"), Password("p"), emailAddress = "nick.bostrom@example.com"),
                AccountInput(Username("iron_man"), Password("p"), emailAddress = "roger@example.com", Name("John"))
            ).map {
                Users.create(it)
                it.toAccount()
            }
            val userId = createVerifiedUsers(1)[0].info.id
            Contacts.create(userId, accounts.map { it.id }.toSet())
            val testContacts = { query: String, accountList: List<Account> ->
                assertEquals(accountList, searchContacts(userId, query).edges.map { it.node })
            }
            testContacts("john", listOf(accounts[0], accounts[1], accounts[3]))
            testContacts("bost", listOf(accounts[2]))
            testContacts("Roger", listOf(accounts[1], accounts[3]))
        }

        @Test
        fun `Contacts should be paginated`() {
            testContactsPagination(ContactsOperationName.SEARCH_CONTACTS)
        }
    }

    @Nested
    inner class SearchMessages {
        @Test
        fun `Searching for a message sent before the private chat was deleted shouldn't be found`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val text = "text"
            Messages.message(user1Id, chatId, MessageText(text))
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(searchMessages(user1Id, text).isEmpty())
        }

        @Test
        fun `Messages should be paginated`() {
            testGroupChatUsersPagination(GroupChatUsersOperationName.SEARCH_MESSAGES)
        }
    }

    @Nested
    inner class SearchUsers {
        @Test
        fun `Users should be searched`() {
            val accounts = listOf(
                AccountInput(Username("iron_man"), Password("p"), "tony@example.com"),
                AccountInput(Username("iron_fist"), Password("p"), "iron_fist@example.com"),
                AccountInput(Username("hulk"), Password("p"), "bruce@example.com")
            ).map {
                Users.create(it)
                it.toAccount()
            }
            assertEquals(accounts.dropLast(1), searchUsers("iron").edges.map { it.node })
        }

        private fun createAccounts(): List<AccountInput> {
            val accounts = listOf(
                AccountInput(Username("iron_man"), Password("p"), "iron.man@example.com"),
                AccountInput(Username("tony_hawk"), Password("p"), "tony.hawk@example.com"),
                AccountInput(Username("lol"), Password("p"), "iron.fist@example.com"),
                AccountInput(Username("another_one"), Password("p"), "another_one@example.com"),
                AccountInput(Username("jo_mama"), Password("p"), "mama@example.com", firstName = Name("Iron")),
                AccountInput(Username("nope"), Password("p"), "nope@example.com", lastName = Name("Irony")),
                AccountInput(Username("black_widow"), Password("p"), "black.widow@example.com"),
                AccountInput(Username("iron_spider"), Password("p"), "iron.spider@example.com")
            )
            accounts.forEach(Users::create)
            return accounts
        }

        @Test
        fun `Users should be paginated`() {
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
    SEARCH_CHATS
}

/** The name of a GraphQL operation which is concerned with the pagination of contacts. */
private enum class ContactsOperationName {
    /** Represents `Query.readContacts`. */
    READ_CONTACTS,

    /** Represents `Query.searchContacts`. */
    SEARCH_CONTACTS
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
    SEARCH_MESSAGES
}

/** Asserts that the [operation] paginates correctly. */
private fun testMessagesPagination(operation: MessagesOperationName) {
    val adminId = createVerifiedUsers(1)[0].info.id
    val chatId = GroupChats.create(listOf(adminId))
    val text = MessageText("t")
    val messageIdList = (1..10).map { Messages.message(adminId, chatId, text) }
    val last = 4
    val cursorIndex = 3
    val pagination = BackwardPagination(last, before = messageIdList[cursorIndex])
    val messages = when (operation) {
        MessagesOperationName.SEARCH_CHAT_MESSAGES -> searchChatMessages(chatId, text.value, pagination, adminId)
        MessagesOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text.value, chatMessagesPagination = pagination).flatMap { it.messages }
        MessagesOperationName.READ_CHATS ->
            readChats(adminId, groupChatMessagesPagination = pagination)[0].messages.edges
        MessagesOperationName.READ_CHAT ->
            readChat(chatId, groupChatMessagesPagination = pagination, userId = adminId).messages.edges
        MessagesOperationName.SEARCH_CHATS -> {
            val title = GroupChats.readChat(chatId, userId = adminId).title.value
            searchChats(adminId, title, groupChatMessagesPagination = pagination)[0].messages.edges
        }
    }
    assertEquals(messageIdList.dropLast(messageIdList.size - cursorIndex).takeLast(last), messages.map { it.cursor })
}

private fun testContactsPagination(operation: ContactsOperationName) {
    val ownerId = createVerifiedUsers(1)[0].info.id
    val userIdList = createVerifiedUsers(10)
    Contacts.create(ownerId, userIdList.map { it.info.id }.toSet())
    val index = 5
    val cursor = readContacts(ownerId).edges[index].cursor
    val first = 3
    val contacts = when (operation) {
        ContactsOperationName.READ_CONTACTS -> readContacts(ownerId, ForwardPagination(first, cursor))
        ContactsOperationName.SEARCH_CONTACTS ->
            searchContacts(ownerId, query = "username", pagination = ForwardPagination(first, cursor))
    }.edges.map { it.node }
    assertEquals(userIdList.subList(index + 1, index + 1 + first).map { it.info }, contacts)
}

private fun testGroupChatUsersPagination(operationName: GroupChatUsersOperationName) {
    val adminId = createVerifiedUsers(1)[0].info.id
    val users = createVerifiedUsers(10)
    val userIdList = users.map { it.info.id }
    val chatId = GroupChats.create(listOf(adminId), userIdList)
    val text = "text"
    Messages.create(adminId, chatId, MessageText(text))
    val first = 3
    val userCursors = GroupChatUsers.read()
    val index = 5
    val pagination = ForwardPagination(first, after = userCursors[index])
    val chat = when (operationName) {
        GroupChatUsersOperationName.READ_CHAT -> readChat(chatId, usersPagination = pagination, userId = adminId)
        GroupChatUsersOperationName.READ_CHATS -> readChats(adminId, usersPagination = pagination)[0]
        GroupChatUsersOperationName.SEARCH_CHATS -> {
            val title = GroupChats.readChat(chatId, userId = adminId).title.value
            searchChats(adminId, title, usersPagination = pagination)[0]
        }
        GroupChatUsersOperationName.SEARCH_MESSAGES ->
            searchMessages(adminId, text, usersPagination = pagination)[0].chat
    } as GroupChat
    assertEquals(userCursors.subList(index + 1, index + 1 + first).map { it }, chat.users.edges.map { it.cursor })
}
