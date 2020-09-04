package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.isUserInChat
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.parseArgument
import com.neelkamath.omniChat.graphql.engine.verifyAuth
import com.neelkamath.omniChat.graphql.routing.*
import graphql.schema.DataFetchingEnvironment
import java.util.*

fun createAccount(env: DataFetchingEnvironment): Placeholder {
    val account = env.parseArgument<AccountInput>("account")
    if (isUsernameTaken(account.username)) throw UsernameTakenException
    if (emailAddressExists(account.emailAddress)) throw EmailAddressTakenException
    if (!hasAllowedDomain(account.emailAddress)) throw InvalidDomainException
    createUser(account)
    return Placeholder
}

fun setOnlineStatus(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Users.setOnlineStatus(env.userId!!, env.getArgument("isOnline"))
    return Placeholder
}

fun createContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val saved = Contacts.readIdList(env.userId!!)
    val userIdList = env.getArgument<List<Int>>("userIdList").filter { it !in saved && it != env.userId!! }.toSet()
    if (userIdList.any { !Users.exists(it) }) throw InvalidContactException
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
 * An [IllegalArgumentException] or [DuplicateStatusException] will be thrown if the [userId] cannot create the [status]
 * on the [messageId].
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
    Stargazers.deleteUserStar(env.userId!!, env.getArgument("messageId"))
    return Placeholder
}

fun createGroupChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val args = env.getArgument<Map<*, *>>("chat")
    @Suppress("UNCHECKED_CAST") val userIdList = args["userIdList"] as List<Int>
    if (userIdList.any { !Users.exists(it) }) throw InvalidUserIdException
    @Suppress("UNCHECKED_CAST") val adminIdList = args["adminIdList"] as List<Int>
    if (!userIdList.containsAll(adminIdList)) throw InvalidAdminIdException
    val chat = GroupChatInput(
        args["title"] as GroupChatTitle,
        args["description"] as GroupChatDescription,
        userIdList + env.userId!!,
        adminIdList + env.userId!!,
        args["isBroadcast"] as Boolean,
        objectMapper.convertValue(args["publicity"] as String)
    )
    return GroupChats.create(chat)
}

fun setTyping(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    TypingStatuses.set(chatId, env.userId!!, env.getArgument("isTyping"))
    return Placeholder
}

@Suppress("DuplicatedCode")
fun createTextMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) throw InvalidMessageIdException
    Messages.createTextMessage(env.userId!!, chatId, env.getArgument("text"), contextMessageId)
    return Placeholder
}

fun setBroadcastStatus(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.setBroadcastStatus(chatId, env.getArgument("isBroadcast"))
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
    if (!GroupChatUsers.canUserLeave(env.userId!!)) throw CannotDeleteAccountException
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
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.updatePic(chatId, pic = null)
    return Placeholder
}

private fun wantsTakenUsername(userId: Int, wantedUsername: Username?): Boolean =
    wantedUsername != null && readUserById(userId).username != wantedUsername && isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: Int, wantedEmail: String?): Boolean =
    wantedEmail != null && readUserById(userId).emailAddress != wantedEmail && emailAddressExists(wantedEmail)

fun sendEmailAddressVerification(env: DataFetchingEnvironment): Placeholder {
    val address = env.getArgument<String>("emailAddress")
    if (!emailAddressExists(address)) throw UnregisteredEmailAddressException
    sendEmailAddressVerification(address)
    return Placeholder
}

fun updateGroupChatTitle(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.updateTitle(chatId, env.getArgument("title"))
    return Placeholder
}

fun updateGroupChatDescription(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.updateDescription(chatId, env.getArgument("description"))
    return Placeholder
}

@Suppress("DuplicatedCode")
fun addGroupChatUsers(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("userIdList")
    if (userIdList.any { !Users.exists(it) }) throw InvalidUserIdException
    GroupChatUsers.addUsers(chatId, userIdList)
    return Placeholder
}

@Suppress("DuplicatedCode")
fun removeGroupChatUsers(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("userIdList")
    if (!GroupChatUsers.canUsersLeave(chatId, userIdList)) throw InvalidUserIdException
    GroupChatUsers.removeUsers(chatId, userIdList)
    return Placeholder
}

@Suppress("DuplicatedCode")
fun makeGroupChatAdmins(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("userIdList")
    if (userIdList.any { !isUserInChat(it, chatId) }) throw InvalidUserIdException
    GroupChatUsers.makeAdmins(chatId, userIdList)
    return Placeholder
}

fun createPollMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val poll = try {
        env.parseArgument<PollInput>("poll")
    } catch (_: IllegalArgumentException) {
        throw InvalidPollException
    }
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) throw InvalidMessageIdException
    Messages.createPollMessage(env.userId!!, chatId, poll, contextMessageId)
    return Placeholder
}

fun setPollVote(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId) || !PollMessages.exists(messageId)) throw InvalidMessageIdException
    val option = env.getArgument<MessageText>("option")
    if (!PollMessages.hasOption(messageId, option)) throw NonexistentOptionException
    val vote = env.getArgument<Boolean>("vote")
    PollMessages.setVote(env.userId!!, messageId, option, vote)
    return Placeholder
}

fun joinGroupChat(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val inviteCode = env.getArgument<UUID>("inviteCode")
    if (!GroupChats.isExistentInviteCode(inviteCode)) throw InvalidInviteCodeException
    GroupChatUsers.addUserViaInvite(env.userId!!, inviteCode)
    return Placeholder
}

fun createGroupChatInviteMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val invitedChatId = env.getArgument<Int>("invitedChatId")
    if (!GroupChats.isInvitable(invitedChatId)) throw InvalidInvitedChatException
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) throw InvalidMessageIdException
    Messages.createGroupChatInviteMessage(env.userId!!, chatId, invitedChatId, contextMessageId)
    return Placeholder
}

fun setInvitability(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (GroupChats.isExistentPublicChat(chatId)) throw InvalidChatIdException
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.setInvitability(chatId, env.getArgument("isInvitable"))
    return Placeholder
}

@Suppress("DuplicatedCode")
fun forwardMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) throw InvalidMessageIdException
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId)) throw InvalidMessageIdException
    Messages.forward(env.userId!!, chatId, messageId, contextMessageId)
    return Placeholder
}

fun createActionMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) throw InvalidChatIdException
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val message = try {
        env.parseArgument<ActionMessageInput>("message")
    } catch (_: IllegalArgumentException) {
        throw InvalidActionException
    }
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) throw InvalidMessageIdException
    Messages.createActionMessage(env.userId!!, chatId, message, contextMessageId)
    return Placeholder
}

fun triggerAction(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId) || !ActionMessages.exists(messageId))
        throw InvalidMessageIdException
    val action = env.getArgument<MessageText>("action")
    if (!ActionMessages.hasAction(messageId, action)) throw InvalidActionException
    ActionMessages.trigger(env.userId!!, messageId, action)
    return Placeholder
}