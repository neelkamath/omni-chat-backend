package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import graphql.schema.DataFetchingEnvironment

fun createAccount(env: DataFetchingEnvironment): Placeholder {
    val account = env.parseArgument<NewAccount>("account")
    if (isUsernameTaken(account.username)) throw UsernameTakenException
    if (emailAddressExists(account.emailAddress)) throw EmailAddressTakenException
    createUser(account)
    return Placeholder
}

fun createContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val saved = Contacts.readIdList(env.userId!!)
    val userIdList = env.getArgument<List<String>>("userIdList").filter { it !in saved && it != env.userId!! }.toSet()
    if (!userIdList.all { userIdExists(it) }) throw InvalidContactException
    Contacts.create(env.userId!!, userIdList)
    return Placeholder
}

fun createStatus(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    val status = env.getArgument<String>("status").let(MessageStatus::valueOf)
    verifyCanCreateStatus(messageId, env.userId!!, status)
    MessageStatuses.create(messageId, env.userId!!, status)
    return Placeholder
}

/**
 * @throws [InvalidMessageIdException] or [DuplicateStatusException] if the [userId] cannot create the [status] on the
 * [messageId].
 */
private fun verifyCanCreateStatus(messageId: Int, userId: String, status: MessageStatus) {
    if (!Messages.exists(messageId) || !Messages.isVisible(messageId, userId)) throw InvalidMessageIdException
    val chatId = Messages.readChatFromMessage(messageId)
    if (!isUserInChat(userId, chatId) || Messages.read(messageId).sender.id == userId) throw InvalidMessageIdException
    if (MessageStatuses.exists(messageId, userId, status)) throw DuplicateStatusException
}

fun createGroupChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val chat = env.parseArgument<NewGroupChat>("chat")
    val userIdList = chat.userIdList.filter { it != env.userId!! }
    if (userIdList.any { !userIdExists(it) }) throw InvalidUserIdException
    return GroupChats.create(env.userId!!, chat)
}

fun createMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val message = env.parseArgument<TextMessage>("text")
    Messages.create(chatId, env.userId!!, message)
    return Placeholder
}

fun createPrivateChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val invitedUserId = env.getArgument<String>("userId")
    if (!userIdExists(invitedUserId) || invitedUserId == env.userId!!) throw InvalidUserIdException
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
    val userIdList = env.getArgument<List<String>>("userIdList")
    Contacts.delete(env.userId!!, userIdList)
    return Placeholder
}

fun deleteMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("id")
    if (!Messages.exists(messageId)) throw InvalidMessageIdException
    val chatId = Messages.readChatFromMessage(messageId)
    if (!isUserInChat(env.userId!!, chatId) ||
        Messages.read(messageId).sender.id != env.userId!! ||
        !Messages.isVisible(messageId, env.userId!!)
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

private fun wantsTakenUsername(userId: String, wantedUsername: Username?): Boolean =
    wantedUsername != null && readUserById(userId).username != wantedUsername && isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: String, wantedEmail: String?): Boolean =
    wantedEmail != null && readUserById(userId).emailAddress != wantedEmail && emailAddressExists(wantedEmail)

fun updateGroupChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val args = env.arguments["update"] as Map<*, *>
    val chatId = args["chatId"] as Int
    if (chatId !in GroupChatUsers.readChatIdList(env.userId!!)) throw InvalidChatIdException
    if (!GroupChats.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val newAdminId = args["newAdminId"] as String?
    if (newAdminId != null && newAdminId !in GroupChatUsers.readUserIdList(chatId)) throw InvalidNewAdminIdException
    val newUserIdList = args["newUserIdList"] as List<*>?
    if (newUserIdList != null && newUserIdList.any { !userIdExists(it as String) }) throw InvalidUserIdException
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