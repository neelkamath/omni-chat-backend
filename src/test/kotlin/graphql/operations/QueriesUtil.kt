package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.testingObjectMapper
import java.util.*

const val READ_TYPING_STATUSES = """
    query ReadTypingStatuses {
        readTypingStatuses {
            $TYPING_STATUS_FRAGMENT
        }
    }
"""

fun readTypingStatuses(userId: Int): List<TypingStatus> {
    val data = executeGraphQlViaEngine(READ_TYPING_STATUSES, userId = userId).data!!["readTypingStatuses"]!!
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
        mapOf("first" to pagination?.first, "after" to pagination?.after.toString()),
        userId,
    ).data!!["readBlockedUsers"]!!
    return testingObjectMapper.convertValue(data)
}

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

fun searchPublicChats(
    query: String,
    usersPagination: ForwardPagination? = null,
    messagesPagination: BackwardPagination? = null,
): List<GroupChat> {
    val data = executeGraphQlViaEngine(
        SEARCH_PUBLIC_CHATS_QUERY,
        mapOf(
            "query" to query,
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
    query ReadStars {
        readStars {
            $STARRED_MESSAGE_FRAGMENT
        }
    }
"""

fun readStars(userId: Int): List<StarredMessage> {
    val data = executeGraphQlViaEngine(READ_STARS_QUERY, userId = userId).data!!["readStars"]!!
    return testingObjectMapper.convertValue(data)
}

const val READ_ONLINE_STATUSES_QUERY = """
    query ReadOnlineStatuses {
        readOnlineStatuses {
            $ONLINE_STATUS_FRAGMENT
        }
    }
"""

fun readOnlineStatuses(userId: Int): List<OnlineStatus> {
    val data = executeGraphQlViaEngine(READ_ONLINE_STATUSES_QUERY, userId = userId)
        .data!!["readOnlineStatuses"]!!
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

fun readChats(
    userId: Int,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
): List<Chat> {
    val chats = executeGraphQlViaEngine(
        READ_CHATS_QUERY,
        mapOf(
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

fun searchChats(
    userId: Int,
    query: String,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
): List<Chat> {
    val chats = executeGraphQlViaEngine(
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

fun searchMessages(
    userId: Int,
    query: String,
    chatMessagesPagination: BackwardPagination? = null,
    privateChatMessagesPagination: BackwardPagination? = null,
    usersPagination: ForwardPagination? = null,
    groupChatMessagesPagination: BackwardPagination? = null,
): List<ChatMessages> {
    val messages = executeGraphQlViaEngine(
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
