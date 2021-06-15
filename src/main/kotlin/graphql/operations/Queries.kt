package com.neelkamath.omniChatBackend.graphql.operations

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChatBackend.buildTokenSet
import com.neelkamath.omniChatBackend.db.BackwardPagination
import com.neelkamath.omniChatBackend.db.CursorType
import com.neelkamath.omniChatBackend.db.ForwardPagination
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.graphql.engine.UnauthorizedException
import com.neelkamath.omniChatBackend.graphql.engine.parseArgument
import com.neelkamath.omniChatBackend.graphql.engine.verifyAuth
import com.neelkamath.omniChatBackend.graphql.routing.Login
import com.neelkamath.omniChatBackend.toLinkedHashSet
import com.neelkamath.omniChatBackend.userId
import graphql.schema.DataFetchingEnvironment
import java.util.*

fun readOnlineStatus(env: DataFetchingEnvironment): ReadOnlineStatusResult {
    val userId = env.getArgument<Int>("userId")
    return if (Users.isExisting(userId)) OnlineStatus(userId) else InvalidUserId
}

fun readTypingUsers(env: DataFetchingEnvironment): List<TypingUsers> {
    env.verifyAuth()
    val chatIdList = PrivateChats.readUserChatIdList(env.userId!!) + GroupChatUsers.readChatIdList(env.userId!!)
    return chatIdList.map(::TypingUsers)
}

fun readAccount(env: DataFetchingEnvironment): Account = Account(env.getArgument("userId"))

fun readChat(env: DataFetchingEnvironment): ReadChatResult {
    val chatId = env.getArgument<Int>("id")
    if (GroupChats.isExistingPublicChat(chatId)) return GroupChat(chatId)
    env.verifyAuth()
    return when (chatId) {
        in PrivateChats.readIdList(env.userId!!) -> PrivateChat(chatId)
        in GroupChatUsers.readChatIdList(env.userId!!) -> GroupChat(chatId)
        else -> InvalidChatId
    }
}

fun readChats(env: DataFetchingEnvironment): ChatsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val chatIdList = PrivateChats.readUserChatIdList(env.userId!!)
        .plus(GroupChatUsers.readChatIdList(env.userId!!))
        .sorted()
    val paginatedChatIdList = chatIdList
        .let { list ->
            if (pagination.after == null) list else list.dropWhile { it <= pagination.after }
        }
        .let { if (pagination.first == null) it else it.take(pagination.first) }
        .toLinkedHashSet()
    return ChatsConnection(
        paginatedChatIdList,
        startCursor = chatIdList.firstOrNull(),
        endCursor = chatIdList.lastOrNull(),
        pagination,
    )
}

fun readContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val userIdList = Contacts.readIdList(env.userId!!, pagination)
    val startCursor = Contacts.readCursor(env.userId!!, CursorType.START)
    val endCursor = Contacts.readCursor(env.userId!!, CursorType.END)
    return AccountsConnection(startCursor, endCursor, userIdList, pagination)
}

fun readMessage(env: DataFetchingEnvironment): Message {
    val messageId = env.getArgument<Int>("messageId")
    return if (Messages.isVisible(env.userId, messageId)) Message.build(messageId) else throw UnauthorizedException
}

fun requestTokenSet(env: DataFetchingEnvironment): RequestTokenSetResult {
    val login = env.parseArgument<Login>("login")
    if (!Users.isUsernameTaken(login.username)) return NonexistingUser
    val userId = Users.readId(login.username)
    if (!Users.hasVerifiedEmailAddress(userId)) return UnverifiedEmailAddress
    if (!Users.isValidLogin(login)) return IncorrectPassword
    return TokenSet(buildTokenSet(userId))
}

fun refreshTokenSet(env: DataFetchingEnvironment): TokenSet {
    val refreshToken = env.getArgument<String>("refreshToken")
    val userId = try {
        JWT.decode(refreshToken).subject.toInt()
    } catch (_: JWTDecodeException) {
        throw UnauthorizedException
    }
    return TokenSet(buildTokenSet(userId))
}

fun searchChatMessages(env: DataFetchingEnvironment): SearchChatMessagesResult {
    val chatId = env.getArgument<Int>("chatId")
    val query = env.getArgument<String>("query")
    val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    val edges = if (env.userId == null && GroupChats.isExistingPublicChat(chatId))
        Messages.searchGroupChat(chatId, query, pagination)
    else {
        env.verifyAuth()
        when (chatId) {
            in PrivateChats.readIdList(env.userId!!) ->
                Messages.searchPrivateChat(chatId, env.userId!!, query, pagination)
            in GroupChatUsers.readChatIdList(env.userId!!) -> Messages.searchGroupChat(chatId, query, pagination)
            else -> return InvalidChatId
        }
    }
    return MessageEdges(edges)
}

fun searchChats(env: DataFetchingEnvironment): ChatsConnection {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val chatIdList = GroupChats.search(env.userId!!, query) + PrivateChats.search(env.userId!!, query)
    val paginatedChatIdList = chatIdList
        .sorted()
        .let { list ->
            if (pagination.after == null) list else list.dropWhile { it <= pagination.after }
        }
        .let { if (pagination.first == null) it else it.take(pagination.first) }
        .toLinkedHashSet()
    return ChatsConnection(
        paginatedChatIdList,
        startCursor = chatIdList.firstOrNull(),
        endCursor = chatIdList.lastOrNull(),
        pagination,
    )
}

fun readStars(env: DataFetchingEnvironment): StarredMessagesConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val messageIdList = Stargazers.readMessageIdList(env.userId!!, pagination)
    return StarredMessagesConnection(messageIdList, pagination)
}

fun searchContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val userIdList = Contacts.search(env.userId!!, env.getArgument("query"))
    return paginateUserIdList(userIdList, pagination)
}

fun searchMessages(env: DataFetchingEnvironment): ChatMessagesConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val query = env.getArgument<String>("query")
    val chats = GroupChats.queryUserChatEdges(env.userId!!, query)
        .plus(PrivateChats.queryUserChatEdges(env.userId!!, query))
        .sortedBy { it.chatId }
    val paginatedChats = chats
        .let { list ->
            if (pagination.after == null) list else list.dropWhile { it.chatId <= pagination.after }
        }
        .let { if (pagination.first == null) it else it.take(pagination.first) }
        .toLinkedHashSet()
    return ChatMessagesConnection(
        startCursor = chats.firstOrNull()?.chatId,
        endCursor = chats.lastOrNull()?.chatId,
        paginatedChats,
        pagination,
    )
}

fun searchUsers(env: DataFetchingEnvironment): AccountsConnection {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val userIdList = Users.search(query)
    return paginateUserIdList(userIdList, pagination)
}

private fun paginateUserIdList(
    userIdList: LinkedHashSet<Int>,
    pagination: ForwardPagination? = null,
): AccountsConnection {
    val paginatedUserIdList = userIdList
        .let { list ->
            if (pagination?.after == null) list else list.dropWhile { it <= pagination.after }
        }
        .let { if (pagination?.first == null) it else it.take(pagination.first) }
        .toLinkedHashSet()
    return AccountsConnection(
        startCursor = userIdList.firstOrNull(),
        endCursor = userIdList.lastOrNull(),
        paginatedUserIdList,
        pagination,
    )
}

fun searchBlockedUsers(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val userIdList = BlockedUsers.search(env.userId!!, query)
    return paginateUserIdList(userIdList, pagination)
}

fun readBlockedUsers(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val userIdList = BlockedUsers.readBlockedUserIdList(env.userId!!, pagination)
    val startCursor = BlockedUsers.readCursor(env.userId!!, CursorType.START)
    val endCursor = BlockedUsers.readCursor(env.userId!!, CursorType.END)
    return AccountsConnection(startCursor, endCursor, userIdList, pagination)
}

fun readGroupChat(env: DataFetchingEnvironment): ReadGroupChatResult {
    val inviteCode = env.getArgument<UUID>("inviteCode")
    return if (GroupChats.isExistingInviteCode(inviteCode))
        GroupChatInfo(GroupChats.readChatIdFromInviteCode(inviteCode)!!)
    else InvalidInviteCode
}

fun searchPublicChats(env: DataFetchingEnvironment): GroupChatsConnection {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    val chatIdList = GroupChats.searchPublicChats(query, pagination)
    val startCursor = GroupChats.readPublicChatsCursor(query, CursorType.START)
    val endCursor = GroupChats.readPublicChatsCursor(query, CursorType.END)
    return GroupChatsConnection(startCursor, endCursor, chatIdList, pagination)
}
