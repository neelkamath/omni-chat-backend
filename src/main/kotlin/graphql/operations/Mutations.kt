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

fun verifyEmailAddress(env: DataFetchingEnvironment): Boolean {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) throw UnregisteredEmailAddressException
    return Users.verifyEmailAddress(address, env.getArgument("verificationCode"))
}

fun blockUser(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val userId = env.getArgument<Int>("id")
    if (!Users.exists(userId)) throw InvalidUserIdException
    BlockedUsers.create(env.userId!!, userId)
    return Placeholder
}

fun unblockUser(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    BlockedUsers.delete(env.userId!!, env.getArgument("id"))
    return Placeholder
}

fun createAccount(env: DataFetchingEnvironment): Placeholder {
    val account = env.parseArgument<AccountInput>("account")
    if (Users.isUsernameTaken(account.username)) throw UsernameTakenException
    if (Users.isEmailAddressTaken(account.emailAddress)) throw EmailAddressTakenException
    if (!hasAllowedDomain(account.emailAddress)) throw InvalidDomainException
    Users.create(account)
    emailEmailAddressVerification(account.emailAddress)
    return Placeholder
}

fun setOnline(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Users.setOnlineStatus(env.userId!!, env.getArgument("isOnline"))
    return Placeholder
}

fun createContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val userIdList = env.getArgument<List<Int>>("idList").filter { Users.exists(it) && it != env.userId!! }
    Contacts.create(env.userId!!, userIdList)
    return Placeholder
}

fun createStatus(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    val status = env.getArgument<String>("status").let(MessageStatus::valueOf)
    if (MessageStatuses.exists(messageId, env.userId!!, status)) return Placeholder
    try {
        MessageStatuses.create(env.userId!!, messageId, status)
    } catch (_: IllegalArgumentException) {
        throw InvalidMessageIdException
    }
    return Placeholder
}

fun deleteStar(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Stargazers.deleteUserStar(env.userId!!, env.getArgument("messageId"))
    return Placeholder
}

fun createGroupChat(env: DataFetchingEnvironment): Int {
    env.verifyAuth()
    val args = env.getArgument<Map<*, *>>("chat")

    @Suppress("UNCHECKED_CAST")
    val userIdList = (args["userIdList"] as List<Int>).filter(Users::exists) + env.userId!!
    @Suppress("UNCHECKED_CAST") val adminIdList = (args["adminIdList"] as List<Int>) + env.userId!!
    if (!userIdList.containsAll(adminIdList)) throw InvalidAdminIdException
    val chat = GroupChatInput(
        args["title"] as GroupChatTitle,
        args["description"] as GroupChatDescription,
        userIdList,
        adminIdList,
        args["isBroadcast"] as Boolean,
        objectMapper.convertValue(args["publicity"] as String),
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

fun setBroadcast(env: DataFetchingEnvironment): Placeholder {
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
    return if (PrivateChats.exists(env.userId!!, invitedUserId)) PrivateChats.readChatId(invitedUserId, env.userId!!)
    else PrivateChats.create(env.userId!!, invitedUserId)
}

fun deleteAccount(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    if (!GroupChatUsers.canUserLeave(env.userId!!)) throw CannotDeleteAccountException
    Users.delete(env.userId!!)
    return Placeholder
}

fun deleteContacts(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val userIdList = env.getArgument<List<Int>>("idList")
    Contacts.delete(env.userId!!, userIdList)
    return Placeholder
}

fun deleteMessage(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("id")
    if (!Messages.exists(messageId)) throw InvalidMessageIdException
    val chatId = Messages.readChatIdFromMessageId(messageId)
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

fun emailPasswordResetCode(env: DataFetchingEnvironment): Placeholder {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) throw UnregisteredEmailAddressException
    emailResetPassword(address)
    return Placeholder
}

fun updateAccount(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val update = env.parseArgument<AccountUpdate>("update")
    if (wantsTakenUsername(env.userId!!, update.username)) throw UsernameTakenException
    if (wantsTakenEmail(env.userId!!, update.emailAddress)) throw EmailAddressTakenException
    val emailAddress = Users.read(env.userId!!).emailAddress
    if (update.emailAddress != null && update.emailAddress != emailAddress) {
        val code = Users.read(emailAddress).emailAddressVerificationCode
        emailNewEmailAddressVerification(update.emailAddress, code)
    }
    Users.update(env.userId!!, update)
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
    wantedUsername != null && Users.read(userId).username != wantedUsername && Users.isUsernameTaken(wantedUsername)

private fun wantsTakenEmail(userId: Int, wantedEmail: String?): Boolean =
    wantedEmail != null && Users.read(userId).emailAddress != wantedEmail && Users.isEmailAddressTaken(wantedEmail)

fun emailEmailAddressVerification(env: DataFetchingEnvironment): Placeholder {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) throw UnregisteredEmailAddressException
    if (Users.read(address).hasVerifiedEmailAddress) throw EmailAddressVerifiedException
    emailEmailAddressVerification(address)
    return Placeholder
}

fun resetPassword(env: DataFetchingEnvironment): Boolean {
    val emailAddress = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(emailAddress)) throw UnregisteredEmailAddressException
    return Users.resetPassword(emailAddress, env.getArgument("passwordResetCode"), env.getArgument("newPassword"))
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

fun addGroupChatUsers(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("idList").filter(Users::exists)
    GroupChatUsers.addUsers(chatId, userIdList)
    return Placeholder
}

fun removeGroupChatUsers(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("idList")
    try {
        GroupChatUsers.removeUsers(chatId, userIdList)
    } catch (_: IllegalArgumentException) {
        throw InvalidUserIdException
    }
    return Placeholder
}

fun makeGroupChatAdmins(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("idList").filter { isUserInChat(it, chatId) }
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
