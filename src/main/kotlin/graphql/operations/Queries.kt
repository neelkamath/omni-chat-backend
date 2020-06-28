package com.neelkamath.omniChat.graphql.operations

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.BackwardPagination
import com.neelkamath.omniChat.db.ForwardPagination
import com.neelkamath.omniChat.db.Users
import com.neelkamath.omniChat.db.chats.GroupChatUsers
import com.neelkamath.omniChat.db.chats.GroupChats
import com.neelkamath.omniChat.db.chats.PrivateChats
import com.neelkamath.omniChat.db.contacts.Contacts
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import graphql.schema.DataFetchingEnvironment

interface ChatDto {
    val id: Int

    fun getMessages(env: DataFetchingEnvironment): MessagesConnection
}

class GroupChatDto(chatId: Int) : ChatDto {
    override val id: Int = chatId
    val title: String
    val description: String?
    val adminId: String

    init {
        val chat = GroupChats.readChat(
            id,
            usersPagination = ForwardPagination(first = 0),
            messagesPagination = BackwardPagination(last = 0)
        )
        title = chat.title
        description = chat.description
        adminId = chat.adminId
    }

    fun getUsers(env: DataFetchingEnvironment): AccountsConnection =
        GroupChatUsers.readUsers(id, ForwardPagination(env.getArgument("first"), env.getArgument("after")))

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readGroupChatConnection(id, pagination)
    }
}

class PrivateChatDto(chatId: Int) : ChatDto {
    override val id: Int = chatId

    fun getUser(env: DataFetchingEnvironment): Account =
        PrivateChats.read(id, env.userId!!, BackwardPagination(last = 0)).user

    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection {
        val pagination = BackwardPagination(env.getArgument("last"), env.getArgument("before"))
        return Messages.readPrivateChatConnection(id, env.userId!!, pagination)
    }
}

sealed class ChatMessagesDto(val chat: ChatDto, private val messageEdges: List<MessageEdge>) {
    fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> {
        val last = env.getArgument<Int?>("last")
        val before = env.getArgument<Int?>("before")
        val edges = if (before == null) messageEdges else messageEdges.filter { it.cursor < before }
        return edges.takeLast(last ?: messageEdges.size)
    }
}

private class SearchGroupChatMessagesDto(
    chatId: Int,
    messageEdges: List<MessageEdge>
) : ChatMessagesDto(GroupChatDto(chatId), messageEdges)

private class SearchPrivateChatMessagesDto(
    chatId: Int,
    messageEdges: List<MessageEdge>
) : ChatMessagesDto(PrivateChatDto(chatId), messageEdges)

fun canDeleteAccount(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return !GroupChats.isNonemptyChatAdmin(env.userId!!)
}

fun isEmailAddressTaken(env: DataFetchingEnvironment): Boolean = emailAddressExists(env.getArgument("emailAddress"))

fun isUsernameTaken(env: DataFetchingEnvironment): Boolean {
    val username = env.getArgument<String>("username")
    return if (username == username.toLowerCase()) isUsernameTaken(username) else throw UsernameNotLowercaseException
}

fun readAccount(env: DataFetchingEnvironment): Account {
    env.verifyAuth()
    return readUserById(env.userId!!)
}

fun readChat(env: DataFetchingEnvironment): ChatDto {
    env.verifyAuth()
    return when (val chatId = env.getArgument<Int>("id")) {
        in PrivateChats.readIdList(env.userId!!) -> PrivateChatDto(chatId)
        in GroupChatUsers.readChatIdList(env.userId!!) -> GroupChatDto(chatId)
        else -> throw InvalidChatIdException
    }
}

fun readChats(env: DataFetchingEnvironment): List<ChatDto> {
    env.verifyAuth()
    return GroupChatUsers.readChatIdList(env.userId!!).map(::GroupChatDto) +
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
        JWT.decode(refreshToken).subject
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
        in GroupChatUsers.readChatIdList(env.userId!!) -> Messages.searchGroupChat(chatId, query, pagination)
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
        .map { GroupChatDto(it.id) }
    val privateChats =
        PrivateChats.search(env.userId!!, query, BackwardPagination(last = 0)).map { PrivateChatDto(it.id) }
    return groupChats + privateChats
}

fun searchContacts(env: DataFetchingEnvironment): AccountsConnection {
    env.verifyAuth()
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Contacts.search(env.userId!!, env.getArgument<String>("query"), pagination)
}

fun searchMessages(env: DataFetchingEnvironment): List<ChatMessagesDto> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val groupChats =
        GroupChats.queryUserChatEdges(env.userId!!, query).map { SearchGroupChatMessagesDto(it.chatId, it.edges) }
    val privateChats =
        PrivateChats.queryUserChatEdges(env.userId!!, query).map { SearchPrivateChatMessagesDto(it.chatId, it.edges) }
    return groupChats + privateChats
}

fun searchUsers(env: DataFetchingEnvironment): AccountsConnection {
    val query = env.getArgument<String>("query")
    val pagination = ForwardPagination(env.getArgument("first"), env.getArgument("after"))
    return Users.search(query, pagination)
}