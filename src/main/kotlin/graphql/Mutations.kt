package com.neelkamath.omniChat.graphql

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import graphql.schema.DataFetchingEnvironment

fun createAccount(env: DataFetchingEnvironment): Boolean {
    val account = getArgument<NewAccount>(env, "account")
    when {
        Auth.usernameExists(account.username) -> throw UsernameTakenException()
        Auth.emailExists(account.email) -> throw EmailTakenException()
        else -> Auth.createUser(account)
    }
    return true
}

fun deleteAccount(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    return if (canDeleteAccount(env.userId)) {
        Auth.deleteUser(env.userId)
        Db.deleteUserData(env.userId)
        true
    } else false
}

fun leaveGroupChat(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    val chatId = env.getArgument<Int>("chatId")
    val newAdminUserId = env.getArgument<String?>("newAdminId")
    val mustSpecifyNewAdmin =
        lazy { GroupChats.isAdmin(env.userId, chatId) && GroupChatUsers.readUserIdList(chatId).size > 1 }
    when {
        chatId !in GroupChats.read(env.userId).map { it.id } -> throw InvalidChatIdException()
        mustSpecifyNewAdmin.value && newAdminUserId == null -> throw MissingNewAdminIdException()
        mustSpecifyNewAdmin.value && newAdminUserId !in GroupChatUsers.readUserIdList(chatId) ->
            throw InvalidNewAdminIdException()
        else -> {
            if (mustSpecifyNewAdmin.value) GroupChats.switchAdmin(chatId, newAdminUserId!!)
            GroupChats.update(GroupChatUpdate(chatId, removedUserIdList = setOf(env.userId)))
        }
    }
    return true
}

fun resetPassword(env: DataFetchingEnvironment): Boolean {
    val email = env.getArgument<String>("email")
    if (!Auth.emailExists(email)) throw UnregisteredEmailException()
    Auth.resetPassword(email)
    return true
}

fun updateAccount(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    val user = getArgument<AccountUpdate>(env, "update")
    when {
        wantsTakenUsername(env.userId, user.username) -> throw UsernameTakenException()
        wantsTakenEmail(env.userId, user.email) -> throw EmailTakenException()
        else -> Auth.updateUser(env.userId, user)
    }
    return true
}

private fun wantsTakenUsername(userId: String, wantedUsername: String?): Boolean =
    wantedUsername != null &&
            Auth.findUserById(userId).username != wantedUsername &&
            Auth.isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: String, wantedEmail: String?): Boolean =
    wantedEmail != null && Auth.findUserById(userId).email != wantedEmail && Auth.emailExists(wantedEmail)

fun verifyEmail(env: DataFetchingEnvironment): Boolean {
    val email = env.getArgument<String>("email")
    if (!Auth.emailExists(email)) throw UnregisteredEmailException()
    Auth.sendEmailVerification(email)
    return true
}

fun updateGroupChat(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    val update = getArgument<GroupChatUpdate>(env, "update")
    when {
        !GroupChats.isUserInChat(env.userId, update.chatId) -> throw InvalidChatIdException()
        !GroupChats.isAdmin(env.userId, update.chatId) -> throw UnauthorizedException()
        update.newAdminId != null && update.newAdminId !in GroupChatUsers.readUserIdList(update.chatId) ->
            throw InvalidNewAdminIdException()
        else -> GroupChats.update(update)
    }
    return true
}

fun createGroupChat(env: DataFetchingEnvironment): Int {
    verifyAuth(env)
    val chat = getArgument<NewGroupChat>(env, "chat")
    val userIdList = chat.userIdList.filter { it != env.userId }
    return when {
        !userIdList.all { Auth.userIdExists(it) } -> throw InvalidUserIdException()
        chat.title.isEmpty() || chat.title.length > GroupChats.maxTitleLength -> throw InvalidTitleLengthException()
        chat.description != null && chat.description.length > GroupChats.maxDescriptionLength ->
            throw InvalidDescriptionLengthException()
        else -> GroupChats.create(env.userId, chat)
    }
}

fun deletePrivateChat(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    val chatId = env.getArgument<Int>("chatId")
    if (chatId !in PrivateChats.read(env.userId).map { it.id }) throw InvalidChatIdException()
    PrivateChatClears.create(chatId, PrivateChats.isCreator(chatId, env.userId))
    return true
}

fun createPrivateChat(env: DataFetchingEnvironment): Int {
    verifyAuth(env)
    val invitedUserId = env.getArgument<String>("userId")
    return when {
        PrivateChats.exists(env.userId, invitedUserId) -> throw ChatExistsException()
        !Auth.userIdExists(invitedUserId) || invitedUserId == env.userId -> throw InvalidUserIdException()
        else -> PrivateChats.create(env.userId, invitedUserId)
    }
}

fun message(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    return true
}

fun deleteContacts(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    val saved = Contacts.read(env.userId)
    val userIdList = env.getArgument<List<String>>("userIdList").filter { it in saved }.toSet()
    Contacts.delete(env.userId, (userIdList))
    return true
}

fun createContacts(env: DataFetchingEnvironment): Boolean {
    verifyAuth(env)
    val saved = Contacts.read(env.userId)
    val userIdList = env.getArgument<List<String>>("userIdList").filter { it !in saved && it != env.userId }.toSet()
    if (!userIdList.all { it in Auth.getUserIdList() }) throw InvalidContactException()
    Contacts.create(env.userId, userIdList)
    return true
}