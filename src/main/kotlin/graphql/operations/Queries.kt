package com.neelkamath.omniChat.graphql.operations

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import com.neelkamath.omniChat.graphql.routing.*
import graphql.schema.DataFetchingEnvironment

interface ChatDto {
    val id: Int

    fun getMessages(env: DataFetchingEnvironment): MessagesConnection
}

/** @param[userId] the user's view of the chat. */
class GroupChatDto(private val userId: Int, chatId: Int) : ChatDto {
    override val id: Int = chatId

    @Suppress("MemberVisibilityCanBePrivate")
    val title: GroupChatTitle

    @Suppress("MemberVisibilityCanBePrivate")
    val description: GroupChatDescription

    @Suppress("unused")
    val adminIdList: List<Int> = GroupChatUsers.readAdminIdList(id)

    val isBroadcast: Boolean

    init {
        val chat = GroupChats.readChat(
            userId,
            id,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0)
        )
        title = chat.title
        description = chat.description
        isBroadcast = chat.isBroadcast
    }

    @Suppress("unused")
    fun getUsers(env: DataFetchingEnvironment): AccountsConnection =
        GroupChatUsers.readUsers(id, ForwardPagination(env.getArgument("first"), env.getArgument("after")))

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readGroupChatConnection(userId, id, pagination)
    }
}

class PrivateChatDto(chatId: Int) : ChatDto {
    override val id: Int = chatId

    @Suppress("unused")
    fun getUser(env: DataFetchingEnvironment): Account =
        PrivateChats.read(id, env.userId!!, BackwardPagination(last = 0)).user

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readPrivateChatConnection(id, env.userId!!, pagination)
    }
}

sealed class ChatMessagesDto(val chat: ChatDto, private val messageEdges: List<MessageEdge>) {
    @Suppress("unused")
    fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> {
        val last = env.getArgument<Int?>("last")
        val before = env.getArgument<Int?>("before")
        val edges = if (before == null) messageEdges else messageEdges.filter { it.cursor < before }
        return edges.takeLast(last ?: messageEdges.size)
    }
}

/** @param[userId] the user searching. */
private class SearchGroupChatMessagesDto(
    userId: Int,
    chatId: Int,
    messageEdges: List<MessageEdge>
) : ChatMessagesDto(GroupChatDto(userId, chatId), messageEdges)

private class SearchPrivateChatMessagesDto(
    chatId: Int,
    messageEdges: List<MessageEdge>
) : ChatMessagesDto(PrivateChatDto(chatId), messageEdges)

fun canDeleteAccount(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return GroupChatUsers.canUserLeave(env.userId!!)
}

fun readOnlineStatuses(env: DataFetchingEnvironment): List<OnlineStatus> {
    env.verifyAuth()
    val userIdList = Contacts.readIdList(env.userId!!) +
            PrivateChats.readOtherUserIdList(env.userId!!) +
            GroupChatUsers.readFellowParticipants(env.userId!!)
    return userIdList.map {
        with(Users.read(it)) { OnlineStatus(it, isOnline, lastOnline) }
    }
}

fun isEmailAddressTaken(env: DataFetchingEnvironment): Boolean =
    emailAddressExists(env.getArgument("emailAddress"))

fun isUsernameTaken(env: DataFetchingEnvironment): Boolean {
    val username = env.parseArgument<Username>("username")
    return isUsernameTaken(username)
}

fun readAccount(env: DataFetchingEnvironment): Account {
    env.verifyAuth()
    return readUserById(env.userId!!)
}

fun readChat(env: DataFetchingEnvironment): ChatDto {
    env.verifyAuth()
    return when (val chatId = env.getArgument<Int>("id")) {
        in PrivateChats.readIdList(env.userId!!) -> PrivateChatDto(chatId)
        in GroupChatUsers.readChatIdList(env.userId!!) -> GroupChatDto(env.userId!!, chatId)
        else -> throw InvalidChatIdException
    }
}

fun readChats(env: DataFetchingEnvironment): List<ChatDto> {
    env.verifyAuth()
    return GroupChatUsers.readChatIdList(env.userId!!).map { GroupChatDto(env.userId!!, it) } +
            PrivateChats.readUserChatIdList(env.userId!!).map(::PrivateChatDto)
}

fun readContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.read(env.userId!!, pagination)
}

fun requestTokenSet(env: DataFetchingEnvironment): TokenSet {
    val login = env.parseArgument<Login>("login")
    if (!isUsernameTaken(login.username)) throw NonexistentUserException
    val userId = readUserByUsername(login.username).id
    if (!isEmailVerified(userId)) throw UnverifiedEmailAddressException
    if (!isValidLogin(login)) throw IncorrectPasswordException
    return buildAuthToken(userId)
}

fun refreshTokenSet(env: DataFetchingEnvironment): TokenSet {
    val refreshToken = env.getArgument<String>("refreshToken")
    val userId = try {
        JWT.decode(refreshToken).subject.toInt()
    } catch (_: JWTDecodeException) {
        throw UnauthorizedException
    }
    return buildAuthToken(userId)
}

fun searchChatMessages(env: DataFetchingEnvironment): List<MessageEdge> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    val query = env.getArgument<String>("query")
    val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    return when (chatId) {
        in PrivateChats.readIdList(env.userId!!) -> Messages.searchPrivateChat(chatId, env.userId!!, query, pagination)
        in GroupChatUsers.readChatIdList(env.userId!!) ->
            Messages.searchGroupChat(env.userId!!, chatId, query, pagination)
        else -> throw InvalidChatIdException
    }
}

fun searchChats(env: DataFetchingEnvironment): List<ChatDto> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val groupChats = GroupChats
        .search(
            env.userId!!,
            query,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0)
        )
        .map { GroupChatDto(env.userId!!, it.id) }
    val privateChats =
        PrivateChats.search(env.userId!!, query, BackwardPagination(last = 0)).map { PrivateChatDto(it.id) }
    return groupChats + privateChats
}

fun readStars(env: DataFetchingEnvironment): List<StarredMessage> {
    env.verifyAuth()
    return Stargazers.read(env.userId!!).map { StarredMessage.build(env.userId!!, it) }
}

fun searchContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.search(env.userId!!, env.getArgument<String>("query"), pagination)
}

fun searchMessages(env: DataFetchingEnvironment): List<ChatMessagesDto> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val groupChats = GroupChats
        .queryUserChatEdges(env.userId!!, query)
        .map { SearchGroupChatMessagesDto(env.userId!!, it.chatId, it.edges) }
    val privateChats = PrivateChats
        .queryUserChatEdges(env.userId!!, query)
        .map { SearchPrivateChatMessagesDto(it.chatId, it.edges) }
    return groupChats + privateChats
}

fun searchUsers(env: DataFetchingEnvironment): AccountsConnection {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Users.search(query, pagination)
}