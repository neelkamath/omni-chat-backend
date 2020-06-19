package com.neelkamath.omniChat.graphql

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import graphql.schema.DataFetchingEnvironment
import org.keycloak.representations.idm.UserRepresentation

sealed class ChatDto(chatId: Int) {
    val id: Int = chatId

    abstract fun getMessages(env: DataFetchingEnvironment): MessagesConnection
}

sealed class GroupChatDto(chatId: Int) : ChatDto(chatId) {
    val title: String
    val description: String?
    val adminId: String
    val users: List<AccountInfo>

    init {
        val chat = GroupChats.readChat(chatId, BackwardPagination(last = 0))
        title = chat.title
        description = chat.description
        adminId = chat.adminId
        users = chat.users
    }
}

sealed class PrivateChatDto(private val chatId: Int) : ChatDto(chatId) {
    fun getUser(env: DataFetchingEnvironment): AccountInfo =
        PrivateChats.read(chatId, env.userId!!, BackwardPagination(last = 0)).user
}

private class ReadGroupChatDto(private val chatId: Int) : GroupChatDto(chatId) {
    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection = Messages.readGroupChatConnection(
        chatId,
        BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    )
}

private class ReadPrivateChatDto(private val chatId: Int) : PrivateChatDto(chatId) {
    override fun getMessages(env: DataFetchingEnvironment): MessagesConnection = Messages.readPrivateChatConnection(
        chatId,
        env.userId!!,
        BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    )
}

sealed class ChatMessagesDto {
    abstract val chat: ChatDto

    abstract fun getMessages(env: DataFetchingEnvironment): List<MessageEdge>
}

private class SearchGroupChatMessagesDto(private val chatId: Int, private val query: String) : ChatMessagesDto() {
    override val chat: ChatDto = ReadGroupChatDto(chatId)

    override fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> = Messages.searchGroupChat(
        chatId,
        query,
        BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    )
}

private class SearchPrivateChatMessagesDto(private val chatId: Int, private val query: String) : ChatMessagesDto() {
    override val chat: ChatDto = ReadPrivateChatDto(chatId)

    override fun getMessages(env: DataFetchingEnvironment): List<MessageEdge> = Messages.searchPrivateChat(
        chatId,
        env.userId!!,
        query,
        BackwardPagination(env.getArgument("last"), env.getArgument("before"))
    )
}

fun canDeleteAccount(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return !GroupChats.isNonemptyChatAdmin(env.userId!!)
}

fun isEmailAddressTaken(env: DataFetchingEnvironment): Boolean = emailAddressExists(env.getArgument("emailAddress"))

fun isUsernameTaken(env: DataFetchingEnvironment): Boolean {
    val username = env.getArgument<String>("username")
    return if (username == username.toLowerCase()) isUsernameTaken(username) else throw UsernameNotLowercaseException
}

fun readAccount(env: DataFetchingEnvironment): AccountInfo {
    env.verifyAuth()
    return findUserById(env.userId!!)
}

fun readChat(env: DataFetchingEnvironment): ChatDto {
    env.verifyAuth()
    return when (val chatId = env.getArgument<Int>("id")) {
        in PrivateChats.readIdList(env.userId!!) -> ReadPrivateChatDto(chatId)
        in GroupChatUsers.readChatIdList(env.userId!!) -> ReadGroupChatDto(chatId)
        else -> throw InvalidChatIdException
    }
}

fun readChats(env: DataFetchingEnvironment): List<ChatDto> {
    env.verifyAuth()
    return GroupChatUsers.readChatIdList(env.userId!!).map(::ReadGroupChatDto) +
            PrivateChats.readUserChatIdList(env.userId!!).map(::ReadPrivateChatDto)
}

fun readContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    env.verifyAuth()
    return Contacts.read(env.userId!!).map(::findUserById)
}

fun requestTokenSet(env: DataFetchingEnvironment): TokenSet {
    val login = env.parseArgument<Login>("login")
    if (!isUsernameTaken(login.username)) throw NonexistentUserException
    val userId = findUserByUsername(login.username).id
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
    return PrivateChats.search(env.userId!!, query, BackwardPagination(last = 0)).map { ReadPrivateChatDto(it.id) } +
            GroupChats.search(env.userId!!, query, BackwardPagination(last = 0)).map { ReadGroupChatDto(it.id) }
}

fun searchContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    return Contacts.read(env.userId!!).map(::findUserById).filter { it.matches(query) }
}

fun searchMessages(env: DataFetchingEnvironment): List<ChatMessagesDto> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    return GroupChats.queryIdList(env.userId!!, query).map { SearchGroupChatMessagesDto(it, query) } +
            PrivateChats.queryIdList(env.userId!!, query).map { SearchPrivateChatMessagesDto(it, query) }
}

/**
 * Case-insensitively searches for the [query] in the [UserRepresentation.username], [UserRepresentation.firstName],
 * [UserRepresentation.lastName], and [UserRepresentation.email].
 */
private fun AccountInfo.matches(query: String): Boolean =
    listOfNotNull(username, firstName, lastName, emailAddress).any { it.contains(query, ignoreCase = true) }

fun searchUsers(env: DataFetchingEnvironment): List<AccountInfo> {
    val query = env.getArgument<String>("query")
    return searchUsers(query)
}