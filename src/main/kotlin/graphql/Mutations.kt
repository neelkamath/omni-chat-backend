package com.neelkamath.omniChat.graphql

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import graphql.schema.DataFetchingEnvironment

fun createAccount(env: DataFetchingEnvironment): Boolean {
    val args = env.arguments["account"] as Map<*, *>
    val username = args["username"] as String
    if (username != username.toLowerCase()) throw UsernameNotLowercaseException
    val account = env.parseArgument<NewAccount>("account")
    when {
        isUsernameTaken(account.username) -> throw UsernameTakenException
        emailAddressExists(account.emailAddress) -> throw EmailAddressTakenException
        else -> createUser(account)
    }
    return true
}

fun createContacts(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val saved = Contacts.read(env.userId!!)
    val userIdList = env.getArgument<List<String>>("userIdList").filter { it !in saved && it != env.userId!! }.toSet()
    if (!userIdList.all { userIdExists(it) }) throw InvalidContactException
    Contacts.create(env.userId!!, userIdList)
    return true
}

fun createDeliveredStatus(env: DataFetchingEnvironment): Boolean = createStatus(env, MessageStatus.DELIVERED)

fun createReadStatus(env: DataFetchingEnvironment): Boolean = createStatus(env, MessageStatus.READ)

/** Implementation for [createDeliveredStatus] and [createReadStatus] (based on the [status]). */
private fun createStatus(env: DataFetchingEnvironment, status: MessageStatus): Boolean {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    verifyCanCreateStatus(messageId, env.userId!!, status)
    MessageStatuses.create(messageId, env.userId!!, status)
    return true
}

/**
 * Throws an [InvalidMessageIdException] or [DuplicateStatusException] if the [userId] cannot create the [status] on the
 * [messageId].
 */
private fun verifyCanCreateStatus(messageId: Int, userId: String, status: MessageStatus) {
    if (!Messages.exists(messageId) || !Messages.isVisible(messageId, userId)) throw InvalidMessageIdException
    val chatId = Messages.findChatFromMessage(messageId)
    if (!isUserInChat(userId, chatId) || Messages.read(messageId).sender.id == userId) throw InvalidMessageIdException
    if (MessageStatuses.exists(messageId, userId, status)) throw DuplicateStatusException
}

fun createGroupChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val chat = env.parseArgument<NewGroupChat>("chat")
    val userIdList = chat.userIdList.filter { it != env.userId!! }
    return when {
        !userIdList.all { userIdExists(it) } -> throw InvalidUserIdException
        chat.title.isEmpty() || chat.title.length > GroupChats.MAX_TITLE_LENGTH -> throw InvalidTitleLengthException
        chat.description != null && chat.description.length > GroupChats.MAX_DESCRIPTION_LENGTH ->
            throw InvalidDescriptionLengthException
        else -> GroupChats.create(env.userId!!, chat)
    }
}

fun createMessage(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val message = env.getArgument<String>("text")
    if (message.length > Messages.MAX_TEXT_LENGTH) throw InvalidMessageLengthException
    Messages.create(chatId, env.userId!!, message)
    return true
}

fun createPrivateChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val invitedUserId = env.getArgument<String>("userId")
    return when {
        !userIdExists(invitedUserId) || invitedUserId == env.userId!! -> throw InvalidUserIdException
        PrivateChats.areInChat(env.userId!!, invitedUserId) -> throw ChatExistsException
        PrivateChats.exists(env.userId!!, invitedUserId) -> PrivateChats.readChatId(invitedUserId, env.userId!!)
        else -> PrivateChats.create(env.userId!!, invitedUserId)
    }
}

fun deleteAccount(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return if (!GroupChats.isNonemptyChatAdmin(env.userId!!)) {
        deleteUserFromDb(env.userId!!)
        deleteUserFromAuth(env.userId!!)
        true
    } else {
        false
    }
}

fun deleteContacts(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val userIdList = env.parseArgument<Set<String>>("userIdList")
    Contacts.delete(env.userId!!, userIdList)
    return true
}

fun deleteMessage(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    val messageId = env.getArgument<Int>("id")
    if (!Messages.exists(messageId, chatId) ||
        Messages.read(messageId).sender.id != env.userId!! ||
        !Messages.isVisible(messageId, env.userId!!)
    ) {
        throw InvalidMessageIdException
    }
    Messages.delete(messageId)
    return true
}

fun deletePrivateChat(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (chatId !in PrivateChats.readIdList(env.userId!!)) throw InvalidChatIdException
    PrivateChatDeletions.create(chatId, env.userId!!)
    unsubscribeFromMessageUpdates(env.userId!!, chatId)
    return true
}

fun leaveGroupChat(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    val newAdminId = env.getArgument<String?>("newAdminId")
    val mustSpecifyNewAdmin =
        lazy { GroupChats.isAdmin(env.userId!!, chatId) && GroupChatUsers.readUserIdList(chatId).size > 1 }
    when {
        chatId !in GroupChatUsers.readChatIdList(env.userId!!) -> throw InvalidChatIdException
        mustSpecifyNewAdmin.value && newAdminId == null -> throw MissingNewAdminIdException
        mustSpecifyNewAdmin.value && newAdminId !in GroupChatUsers.readUserIdList(chatId) ->
            throw InvalidNewAdminIdException
        else -> {
            if (mustSpecifyNewAdmin.value) GroupChats.setAdmin(chatId, newAdminId!!)
            val update = GroupChatUpdate(chatId, removedUserIdList = setOf(env.userId!!))
            GroupChats.update(update)
        }
    }
    return true
}

fun resetPassword(env: DataFetchingEnvironment): Boolean {
    val address = env.getArgument<String>("emailAddress")
    if (!emailAddressExists(address)) throw UnregisteredEmailAddressException
    resetPassword(address)
    return true
}

fun updateAccount(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val update = env.parseArgument<AccountUpdate>("update")
    when {
        wantsTakenUsername(env.userId!!, update.username) -> throw UsernameTakenException
        wantsTakenEmail(env.userId!!, update.emailAddress) -> throw EmailAddressTakenException
        else -> updateUser(env.userId!!, update)
    }
    return true
}

private fun wantsTakenUsername(userId: String, wantedUsername: String?): Boolean =
    wantedUsername != null && findUserById(userId).username != wantedUsername && isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: String, wantedEmail: String?): Boolean =
    wantedEmail != null && findUserById(userId).emailAddress != wantedEmail && emailAddressExists(wantedEmail)

fun updateGroupChat(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val update = env.parseArgument<GroupChatUpdate>("update")
    when {
        update.chatId !in GroupChatUsers.readChatIdList(env.userId!!) -> throw InvalidChatIdException
        !GroupChats.isAdmin(env.userId!!, update.chatId) -> throw UnauthorizedException
        update.newAdminId != null && update.newAdminId !in GroupChatUsers.readUserIdList(update.chatId) ->
            throw InvalidNewAdminIdException
        else -> GroupChats.update(update)
    }
    return true
}

fun sendEmailAddressVerification(env: DataFetchingEnvironment): Boolean {
    val address = env.getArgument<String>("emailAddress")
    if (!emailAddressExists(address)) throw UnregisteredEmailAddressException
    sendEmailAddressVerification(address)
    return true
}