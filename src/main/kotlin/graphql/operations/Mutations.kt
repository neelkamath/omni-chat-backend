package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import graphql.schema.DataFetchingEnvironment

fun createAccount(env: DataFetchingEnvironment): Placeholder {
    val account = env.parseArgument<AccountInput>("account")
    if (isUsernameTaken(account.username)) throw UsernameTakenException
    if (emailAddressExists(account.emailAddress)) throw EmailAddressTakenException
    createUser(account)
    return Placeholder
}

fun setOnlineStatus(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Users.setOnlineStatus(env.userId!!, env.getArgument<Boolean>("isOnline"))
    return Placeholder
}

fun createContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val saved = Contacts.readIdList(env.userId!!)
    val userIdList = env.getArgument<List<Int>>("userIdList").filter { it !in saved && it != env.userId!! }.toSet()
    if (!userIdList.all { Users.exists(it) }) throw InvalidContactException
    Contacts.create(env.userId!!, userIdList)
    return Placeholder
}

fun createStatus(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    val status = env.getArgument<String>("status").let(MessageStatus::valueOf)
    verifyCanCreateStatus(messageId, env.userId!!, status)
    MessageStatuses.create(env.userId!!, messageId, status)
    return Placeholder
}

/**
 * @throws [InvalidMessageIdException] or [DuplicateStatusException] if the [userId] cannot create the [status] on the
 * [messageId].
 */
private fun verifyCanCreateStatus(messageId: Int, userId: Int, status: MessageStatus) {
    if (!Messages.exists(messageId) || !Messages.isVisible(userId, messageId)) throw InvalidMessageIdException
    val chatId = Messages.readChatFromMessage(messageId)
    if (!isUserInChat(userId, chatId) || Messages.readMessage(userId, messageId).sender.id == userId)
        throw InvalidMessageIdException
    if (MessageStatuses.exists(messageId, userId, status)) throw DuplicateStatusException
}

fun deleteStar(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Stargazers.deleteUserStar(env.userId!!, env.getArgument<Int>("messageId"))
    return Placeholder
}

fun createGroupChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val chat = env.parseArgument<GroupChatInput>("chat")
    val userIdList = chat.userIdList.filter { it != env.userId!! }
    if (userIdList.any { !Users.exists(it) }) throw InvalidUserIdException
    return GroupChats.create(env.userId!!, chat)
}

fun setTyping(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    TypingStatuses.set(chatId, env.userId!!, env.getArgument<Boolean>("isTyping"))
    return Placeholder
}

fun createMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val message = env.parseArgument<TextMessage>("text")
    Messages.create(env.userId!!, chatId, message)
    return Placeholder
}

fun star(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId)) throw InvalidMessageIdException
    Stargazers.create(env.userId!!, messageId)
    return Placeholder
}

fun createPrivateChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val invitedUserId = env.getArgument<Int>("userId")
    if (!Users.exists(invitedUserId) || invitedUserId == env.userId!!) throw InvalidUserIdException
    if (PrivateChats.areInChat(env.userId!!, invitedUserId)) throw ChatExistsException
    return if (PrivateChats.exists(env.userId!!, invitedUserId)) PrivateChats.readChatId(invitedUserId, env.userId!!)
    else PrivateChats.create(env.userId!!, invitedUserId)
}

fun deleteAccount(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    if (GroupChats.isNonemptyChatAdmin(env.userId!!)) throw CannotDeleteAccountException
    deleteUser(env.userId!!)
    return Placeholder
}

fun deleteContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val userIdList = env.getArgument<List<Int>>("userIdList")
    Contacts.delete(env.userId!!, userIdList)
    return Placeholder
}

fun deleteMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("id")
    if (!Messages.exists(messageId)) throw InvalidMessageIdException
    val chatId = Messages.readChatFromMessage(messageId)
    if (!isUserInChat(env.userId!!, chatId) ||
        Messages.readMessage(env.userId!!, messageId).sender.id != env.userId!! ||
        !Messages.isVisible(env.userId!!, messageId)
    ) {
        throw InvalidMessageIdException
    }
    Messages.delete(messageId)
    return Placeholder
}

fun deletePrivateChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (chatId !in PrivateChats.readIdList(env.userId!!)) throw InvalidChatIdException
    PrivateChatDeletions.create(chatId, env.userId!!)
    return Placeholder
}

fun leaveGroupChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (chatId !in GroupChatUsers.readChatIdList(env.userId!!)) throw InvalidChatIdException
    if (GroupChats.isAdmin(env.userId!!, chatId) && GroupChatUsers.readUserIdList(chatId).size > 1)
        throw AdminCannotLeaveException
    GroupChatUsers.removeUsers(chatId, env.userId!!)
    return Placeholder
}

fun resetPassword(env: DataFetchingEnvironment): Placeholder {
    val address = env.getArgument<String>("emailAddress")
    if (!emailAddressExists(address)) throw UnregisteredEmailAddressException
    resetPassword(address)
    return Placeholder
}

fun updateAccount(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val update = env.parseArgument<AccountUpdate>("update")
    if (wantsTakenUsername(env.userId!!, update.username)) throw UsernameTakenException
    if (wantsTakenEmail(env.userId!!, update.emailAddress)) throw EmailAddressTakenException
    updateUser(env.userId!!, update)
    return Placeholder
}

fun deleteProfilePic(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Users.updatePic(env.userId!!, pic = null)
    return Placeholder
}

fun deleteGroupChatPic(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!Chats.exists(chatId)) throw InvalidChatIdException
    if (!GroupChats.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.updatePic(chatId, pic = null)
    return Placeholder
}

private fun wantsTakenUsername(userId: Int, wantedUsername: Username?): Boolean =
    wantedUsername != null && readUserById(userId).username != wantedUsername && isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: Int, wantedEmail: String?): Boolean =
    wantedEmail != null && readUserById(userId).emailAddress != wantedEmail && emailAddressExists(wantedEmail)

fun updateGroupChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val args = env.arguments["update"] as Map<*, *>
    val chatId = args["chatId"] as Int
    if (chatId !in GroupChatUsers.readChatIdList(env.userId!!)) throw InvalidChatIdException
    if (!GroupChats.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val newAdminId = args["newAdminId"] as Int?
    if (newAdminId != null && newAdminId !in GroupChatUsers.readUserIdList(chatId)) throw InvalidNewAdminIdException
    val newUserIdList = args["newUserIdList"] as List<*>?
    if (newUserIdList != null && newUserIdList.any { !Users.exists(it as Int) }) throw InvalidUserIdException
    val removedUserIdList = args["removedUserIdList"] as List<*>?
    if (newUserIdList != null && removedUserIdList != null && newUserIdList.intersect(removedUserIdList).isNotEmpty())
        throw InvalidGroupChatUsersException
    val update = env.parseArgument<GroupChatUpdate>("update")
    GroupChats.update(update)
    return Placeholder
}

fun sendEmailAddressVerification(env: DataFetchingEnvironment): Placeholder {
    val address = env.getArgument<String>("emailAddress")
    if (!emailAddressExists(address)) throw UnregisteredEmailAddressException
    sendEmailAddressVerification(address)
    return Placeholder
}