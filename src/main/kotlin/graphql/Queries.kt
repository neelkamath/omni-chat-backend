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

fun isUsernameTaken(env: DataFetchingEnvironment): Boolean = isUsernameTaken(env.getArgument<String>("username"))

fun readAccount(env: DataFetchingEnvironment): AccountInfo {
    env.verifyAuth()
    return buildAccountInfo(env.userId!!)
}

fun readChats(env: DataFetchingEnvironment): List<Chat> {
    env.verifyAuth()
    val groupChats = GroupChats.read(env.userId!!)
    val privateChats = PrivateChats.read(env.userId!!)
    return groupChats + privateChats
}

fun readContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    env.verifyAuth()
    return Contacts.read(env.userId!!).map(::buildAccountInfo)
}

fun requestTokenSet(env: DataFetchingEnvironment): TokenSet {
    val login = env.parseArgument<Login>("login")
    return when {
        !isUsernameTaken(login.username) -> throw NonexistentUserException()
        !findUserByUsername(login.username).isEmailVerified -> throw UnverifiedEmailAddressException()
        else -> {
            if (!isValidLogin(login)) throw IncorrectCredentialsException()
            val userId = findUserByUsername(login.username).id
            buildAuthToken(userId)
        }
    }
}

fun refreshTokenSet(env: DataFetchingEnvironment): TokenSet {
    val refreshToken = env.getArgument<String>("refreshToken")
    val userId = try {
        JWT.decode(refreshToken).subject
    } catch (exception: JWTDecodeException) {
        throw UnauthorizedException()
    }
    return buildAuthToken(userId)
}

fun searchChatMessages(env: DataFetchingEnvironment): List<Message> {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException()
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
    return Contacts.read(env.userId!!).map(::findUserById).filter { it.matches(query) }.map { buildAccountInfo(it.id) }
}

/**
 * Case-insensitively matches the [UserRepresentation.username], [UserRepresentation.firstName],
 * [UserRepresentation.lastName], and [UserRepresentation.email] with the [query].
 */
private fun UserRepresentation.matches(query: String): Boolean =
    listOfNotNull(username, firstName, lastName, email).any { it.contains(query, ignoreCase = true) }

fun searchUsers(env: DataFetchingEnvironment): List<AccountInfo> {
    val query = env.getArgument<String>("query")
    return searchUsers(query).map { buildAccountInfo(it.id) }
}

private fun buildAccountInfo(userId: String): AccountInfo =
    with(findUserById(userId)) { AccountInfo(id, username, email, firstName, lastName) }