package com.neelkamath.omniChat.graphql

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import graphql.schema.DataFetchingEnvironment
import org.keycloak.representations.idm.UserRepresentation

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
    return readChat(chatId, env.userId!!)
}

fun readChats(env: DataFetchingEnvironment): List<Chat> {
    env.verifyAuth()
    return GroupChats.read(env.userId!!) + PrivateChats.read(env.userId!!)
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

fun searchChatMessages(env: DataFetchingEnvironment): List<Message> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val query = env.getArgument<String>("query")
    return Messages.search(chatId, query)
}

fun searchChats(env: DataFetchingEnvironment): List<Chat> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    return PrivateChats.search(env.userId!!, query) + GroupChats.search(env.userId!!, query)
}

fun searchContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    return Contacts.read(env.userId!!).map(::findUserById).filter { it.matches(query) }
}

fun searchMessages(env: DataFetchingEnvironment): List<ChatMessages> {
    env.verifyAuth()
    val query = env.getArgument<String>("query")
    return Messages.search(env.userId!!, query)
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