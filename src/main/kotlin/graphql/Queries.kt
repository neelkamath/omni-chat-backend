package com.neelkamath.omniChat.graphql

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Contacts
import com.neelkamath.omniChat.db.GroupChats
import com.neelkamath.omniChat.db.PrivateChatClears
import com.neelkamath.omniChat.db.PrivateChats
import graphql.schema.DataFetchingEnvironment
import org.keycloak.representations.idm.UserRepresentation

fun canDeleteAccount(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    return canDeleteAccount(env.userId)
}

fun isEmailTaken(env: DataFetchingEnvironment): Boolean = Auth.emailExists(env.getArgument("email"))

fun isUsernameTaken(env: DataFetchingEnvironment): Boolean = Auth.isUsernameTaken(env.getArgument("username"))

fun readAccount(env: DataFetchingEnvironment): AccountInfo {
    verifyAuth(env)
    return buildAccountInfo(env.userId)
}

fun readChats(env: DataFetchingEnvironment): List<Chat> {
    verifyAuth(env)
    val groupChats = GroupChats.read(env.userId)
    val privateChats = PrivateChats
        .read(env.userId)
        .filter {
            val isCreator = PrivateChats.isCreator(it.id, env.userId)
            !PrivateChatClears.hasCleared(isCreator, it.id)
        }
        .map { PrivateChat(it.id, if (it.creatorUserId == env.userId) it.invitedUserId else it.creatorUserId) }
    return groupChats + privateChats
}

fun readContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    verifyAuth(env)
    return Contacts.read(env.userId).map { userId -> buildAccountInfo(userId) }
}

fun requestJwt(env: DataFetchingEnvironment): AuthToken {
    val login = getArgument<Login>(env, "login")
    return when {
        !Auth.usernameExists(login.username) -> throw NonexistentUserException()
        !Auth.findUserByUsername(login.username).isEmailVerified -> throw UnverifiedEmailException()
        else -> {
            val token = Auth.getToken(login) ?: throw IncorrectPasswordException()
            val userId = Auth.findUserByUsername(login.username).id
            Jwt.buildAuthToken(userId, token)
        }
    }
}

fun searchChats(env: DataFetchingEnvironment): List<Chat> {
    verifyAuth(env)
    val query = env.getArgument<String>("query")
    val privateChats = PrivateChats.search(env.userId, query)
    val groupChats = GroupChats.search(env.userId, query)
    return privateChats + groupChats
}

fun searchContacts(env: DataFetchingEnvironment): List<AccountInfo> {
    verifyAuth(env)
    val query = env.getArgument<String>("query")
    val contacts = Contacts.read(env.userId).map { Auth.findUserById(it) }.filter { matches(query, it) }
    return contacts.map { it.id }.toSet().map { userId -> buildAccountInfo(userId) }
}

private fun matches(query: String, user: UserRepresentation): Boolean =
    with(user) { listOfNotNull(username, firstName, lastName, email) }.any { it.contains(query, ignoreCase = true) }

fun searchUsers(env: DataFetchingEnvironment): List<AccountInfo> =
    Auth.searchUsers(env.getArgument("query")).map { it.id }.toSet().map { userId -> buildAccountInfo(userId) }