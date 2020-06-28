package com.neelkamath.omniChat.graphql.operations

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Users
import com.neelkamath.omniChat.db.chats.GroupChatUsers
import com.neelkamath.omniChat.db.chats.GroupChats
import com.neelkamath.omniChat.db.chats.PrivateChatDeletions
import com.neelkamath.omniChat.db.chats.PrivateChats
import com.neelkamath.omniChat.db.contacts.Contacts
import com.neelkamath.omniChat.db.deleteUserFromDb
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.messages.MessageStatuses
import com.neelkamath.omniChat.db.messages.Messages
import com.neelkamath.omniChat.db.messages.unsubscribeUserFromMessageUpdates
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import graphql.schema.DataFetchingEnvironment

fun createAccount(env: DataFetchingEnvironment): Boolean {
    val args = env.arguments["account"] as Map<*, *>
    val username = args["username"] as String
    if (username != username.toLowerCase()) throw UsernameNotLowercaseException
    val account = env.parseArgument<NewAccount>("account")
    if (isUsernameTaken(account.username)) throw UsernameTakenException
    if (emailAddressExists(account.emailAddress)) throw EmailAddressTakenException
    Users.create(userId = createUser(account))
    return true
}

fun createContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val saved = Contacts.readIdList(env.userId!!)
    val userIdList = env.getArgument<List<String>>("userIdList").filter { it !in saved && it != env.userId!! }.toSet()
    if (!userIdList.all { userIdExists(it) }) throw InvalidContactException
    Contacts.create(env.userId!!, userIdList)
    return Placeholder
}

fun createDeliveredStatus(env: DataFetchingEnvironment): Placeholder = createStatus(env, MessageStatus.DELIVERED)

fun createReadStatus(env: DataFetchingEnvironment): Placeholder = createStatus(env, MessageStatus.READ)

/** Implementation for [createDeliveredStatus] and [createReadStatus] (based on the [status]). */
private fun createStatus(env: DataFetchingEnvironment, status: MessageStatus): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    verifyCanCreateStatus(messageId, env.userId!!, status)
    MessageStatuses.create(messageId, env.userId!!, status)
    return Placeholder
}

/**
 * Throws an [InvalidMessageIdException] or [DuplicateStatusException] if the [userId] cannot create the [status] on the
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
    if (!userIdList.all { userIdExists(it) }) throw InvalidUserIdException
    if (chat.title.isEmpty() || chat.title.length > GroupChats.MAX_TITLE_LENGTH) throw InvalidTitleLengthException
    if (chat.description != null && chat.description.length > GroupChats.MAX_DESCRIPTION_LENGTH)
        throw InvalidDescriptionLengthException
    return GroupChats.create(env.userId!!, chat)
}

fun createMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val message = env.getArgument<String>("text")
    if (message.length > Messages.MAX_TEXT_LENGTH) throw InvalidMessageLengthException
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
    deleteUserFromDb(env.userId!!)
    deleteUserFromAuth(env.userId!!)
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
        !Messages.existsInChat(messageId, chatId) ||
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
    unsubscribeUserFromMessageUpdates(env.userId!!, chatId)
    return Placeholder
}

fun leaveGroupChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    val newAdminId = env.getArgument<String?>("newAdminId")
    if (chatId !in GroupChatUsers.readChatIdList(env.userId!!)) throw InvalidChatIdException
    val mustSpecifyNewAdmin = GroupChats.isAdmin(env.userId!!, chatId) && GroupChatUsers.readUserIdList(chatId).size > 1
    if (mustSpecifyNewAdmin && newAdminId == null) throw MissingNewAdminIdException
    if (mustSpecifyNewAdmin && newAdminId !in GroupChatUsers.readUserIdList(chatId)) throw InvalidNewAdminIdException
    if (mustSpecifyNewAdmin) GroupChats.setAdmin(chatId, newAdminId!!)
    val update = GroupChatUpdate(chatId, removedUserIdList = listOf(env.userId!!))
    GroupChats.update(update)
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

private fun wantsTakenUsername(userId: String, wantedUsername: String?): Boolean =
    wantedUsername != null && readUserById(userId).username != wantedUsername && isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: String, wantedEmail: String?): Boolean =
    wantedEmail != null && readUserById(userId).emailAddress != wantedEmail && emailAddressExists(wantedEmail)

fun updateGroupChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val update = env.parseArgument<GroupChatUpdate>("update")
    if (update.chatId !in GroupChatUsers.readChatIdList(env.userId!!)) throw InvalidChatIdException
    if (!GroupChats.isAdmin(env.userId!!, update.chatId)) throw UnauthorizedException
    if (update.newAdminId != null && update.newAdminId !in GroupChatUsers.readUserIdList(update.chatId))
        throw InvalidNewAdminIdException
    GroupChats.update(update)
    return Placeholder
}

fun sendEmailAddressVerification(env: DataFetchingEnvironment): Placeholder {
    val address = env.getArgument<String>("emailAddress")
    if (!emailAddressExists(address)) throw UnregisteredEmailAddressException
    sendEmailAddressVerification(address)
    return Placeholder
}