package com.neelkamath.omniChat.graphql

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import graphql.schema.DataFetchingEnvironment
import org.keycloak.representations.idm.UserRepresentation

private fun readBackwardPagination(env: DataFetchingEnvironment): BackwardPagination =
    BackwardPagination(env.parseFieldArgument("messages", "last"), env.parseFieldArgument("messages", "before"))

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

fun readChat(env: DataFetchingEnvironment): Chat {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("id")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    return readChat(chatId, env.userId!!, readBackwardPagination(env))
}

fun readChats(env: DataFetchingEnvironment): List<Chat> {
    env.verifyAuth()
    val pagination = readBackwardPagination(env)
    return GroupChats.read(env.userId!!, pagination) + PrivateChats.read(env.userId!!, pagination)
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
    val pagination = readBackwardPagination(env)
    return when (chatId) {
        in PrivateChats.readIdList(env.userId!!) -> Messages.searchPrivateChat(chatId, env.userId!!, query, pagination)
        in GroupChatUsers.readChatIdList(env.userId!!) -> Messages.searchGroupChat(chatId, query, pagination)
        else -> throw InvalidChatIdException
    }
}

fun searchChats(env: DataFetchingEnvironment): List<Chat> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val pagination = readBackwardPagination(env)
    return PrivateChats.search(env.userId!!, query, pagination) + GroupChats.search(env.userId!!, query, pagination)
}

fun searchContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    return Contacts.read(env.userId!!).map(::findUserById).filter { it.matches(query) }
}

fun searchMessages(env: DataFetchingEnvironment): List<ChatMessages> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    val pagination = readBackwardPagination(env)
    return Messages.search(env.userId!!, query, pagination)
}

/**
 * Case-insensitively [query]s the [UserRepresentation.username], [UserRepresentation.firstName],
 * [UserRepresentation.lastName], and [UserRepresentation.email].
 */
private fun AccountInfo.matches(query: String): Boolean =
    listOfNotNull(username, firstName, lastName, emailAddress).any { it.contains(query, ignoreCase = true) }

fun searchUsers(env: DataFetchingEnvironment): List<AccountInfo> {
    val query = env.getArgument<String>("query")
    return searchUsers(query)
}