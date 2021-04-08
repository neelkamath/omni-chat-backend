package com.neelkamath.omniChatBackend.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.deleteUser
import com.neelkamath.omniChatBackend.db.isUserInChat
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.engine.UnauthorizedException
import com.neelkamath.omniChatBackend.graphql.engine.parseArgument
import com.neelkamath.omniChatBackend.graphql.engine.verifyAuth
import com.neelkamath.omniChatBackend.graphql.routing.*
import graphql.schema.DataFetchingEnvironment
import java.util.*

fun verifyEmailAddress(env: DataFetchingEnvironment): VerifyEmailAddressResult? {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) return UnregisteredEmailAddress
    val isVerified = Users.verifyEmailAddress(address, env.getArgument("verificationCode"))
    return if (isVerified) null else InvalidVerificationCode
}

fun blockUser(env: DataFetchingEnvironment): InvalidUserId? {
    env.verifyAuth()
    val userId = env.getArgument<Int>("id")
    if (!Users.isExisting(userId)) return InvalidUserId
    BlockedUsers.create(env.userId!!, userId)
    return null
}

fun unblockUser(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return BlockedUsers.delete(env.userId!!, env.getArgument("id"))
}

fun createAccount(env: DataFetchingEnvironment): CreateAccountResult? {
    val account = env.parseArgument<AccountInput>("account")
    if (Users.isUsernameTaken(account.username)) return UsernameTaken
    if (Users.isEmailAddressTaken(account.emailAddress)) return EmailAddressTaken
    if (!hasAllowedDomain(account.emailAddress)) return InvalidDomain
    Users.create(account)
    emailEmailAddressVerification(account.emailAddress)
    return null
}

fun setOnline(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Users.setOnlineStatus(env.userId!!, env.getArgument("isOnline"))
    return Placeholder
}

fun createContact(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val userId = env.getArgument<Int>("id")
    return if (Users.isExisting(userId)) Contacts.create(env.userId!!, userId) else false
}

fun createStatus(env: DataFetchingEnvironment): InvalidMessageId? {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    val status = env.getArgument<String>("status").let(MessageStatus::valueOf)
    if (MessageStatuses.isExisting(messageId, env.userId!!, status)) return null
    try {
        MessageStatuses.create(env.userId!!, messageId, status)
    } catch (_: IllegalArgumentException) {
        return InvalidMessageId
    }
    return null
}

fun unstar(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    Stargazers.deleteUserStar(env.userId!!, env.getArgument("messageId"))
    return Placeholder
}

fun createGroupChat(env: DataFetchingEnvironment): CreateGroupChatResult {
    env.verifyAuth()
    val args = env.getArgument<Map<*, *>>("chat")

    @Suppress("UNCHECKED_CAST")
    val userIdList = (args["userIdList"] as List<Int>).filter(Users::isExisting) + env.userId!!
    @Suppress("UNCHECKED_CAST") val adminIdList = (args["adminIdList"] as List<Int>) + env.userId!!
    if (!userIdList.containsAll(adminIdList)) return InvalidAdminId
    val chat = GroupChatInput(
        args["title"] as GroupChatTitle,
        args["description"] as GroupChatDescription,
        userIdList,
        adminIdList,
        args["isBroadcast"] as Boolean,
        objectMapper.convertValue(args["publicity"] as String),
    )
    return CreatedChatId(GroupChats.create(chat))
}

fun setTyping(env: DataFetchingEnvironment): InvalidChatId? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) return InvalidChatId
    TypingStatuses.update(chatId, env.userId!!, env.getArgument("isTyping"))
    return null
}

@Suppress("DuplicatedCode")
fun createTextMessage(env: DataFetchingEnvironment): CreateTextMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) return InvalidMessageId
    Messages.createTextMessage(env.userId!!, chatId, env.getArgument("text"), contextMessageId)
    return null
}

@Suppress("DuplicatedCode")
fun forwardMessage(env: DataFetchingEnvironment): ForwardMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) return InvalidMessageId
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId)) return InvalidMessageId
    Messages.forward(env.userId!!, chatId, messageId, contextMessageId)
    return null
}

fun setBroadcast(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.setBroadcastStatus(chatId, env.getArgument("isBroadcast"))
    return Placeholder
}

fun star(env: DataFetchingEnvironment): InvalidMessageId? {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId)) return InvalidMessageId
    Stargazers.create(env.userId!!, messageId)
    return null
}

fun createPrivateChat(env: DataFetchingEnvironment): CreatePrivateChatResult {
    env.verifyAuth()
    val invitedUserId = env.getArgument<Int>("userId")
    if (!Users.isExisting(invitedUserId) || invitedUserId == env.userId!!) return InvalidUserId
    val id =
        if (PrivateChats.isExisting(env.userId!!, invitedUserId)) PrivateChats.readChatId(invitedUserId, env.userId!!)
        else PrivateChats.create(env.userId!!, invitedUserId)
    return CreatedChatId(id)
}

fun deleteAccount(env: DataFetchingEnvironment): CannotDeleteAccount? {
    env.verifyAuth()
    if (!GroupChatUsers.canUserLeave(env.userId!!)) return CannotDeleteAccount
    deleteUser(env.userId!!)
    return null
}

fun deleteContact(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    return Contacts.delete(env.userId!!, env.getArgument("id"))
}

fun deleteMessage(env: DataFetchingEnvironment): InvalidMessageId? {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("id")
    if (!Messages.isExisting(messageId)) return InvalidMessageId
    val chatId = Messages.readChatIdFromMessageId(messageId)
    if (!isUserInChat(env.userId!!, chatId) ||
        Messages.readMessage(env.userId!!, messageId).sender.id != env.userId!! ||
        !Messages.isVisible(env.userId!!, messageId)
    ) {
        return InvalidMessageId
    }
    Messages.delete(messageId)
    return null
}

fun deletePrivateChat(env: DataFetchingEnvironment): InvalidChatId? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (chatId !in PrivateChats.readIdList(env.userId!!)) return InvalidChatId
    PrivateChatDeletions.create(chatId, env.userId!!)
    return null
}

fun emailPasswordResetCode(env: DataFetchingEnvironment): UnregisteredEmailAddress? {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) return UnregisteredEmailAddress
    emailResetPassword(address)
    return null
}

fun updateAccount(env: DataFetchingEnvironment): UpdateAccountResult? {
    env.verifyAuth()
    val update = env.parseArgument<AccountUpdate>("update")
    if (wantsTakenUsername(env.userId!!, update.username)) return UsernameTaken
    if (wantsTakenEmail(env.userId!!, update.emailAddress)) return EmailAddressTaken
    val emailAddress = Users.read(env.userId!!).emailAddress
    if (update.emailAddress != null && update.emailAddress != emailAddress) {
        val code = Users.read(emailAddress).emailAddressVerificationCode
        emailNewEmailAddressVerification(update.emailAddress, code)
    }
    Users.update(env.userId!!, update)
    return null
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

fun emailEmailAddressVerification(env: DataFetchingEnvironment): EmailEmailAddressVerificationResult? {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) return UnregisteredEmailAddress
    if (Users.read(address).hasVerifiedEmailAddress) return EmailAddressVerified
    emailEmailAddressVerification(address)
    return null
}

fun resetPassword(env: DataFetchingEnvironment): ResetPasswordResult? {
    val emailAddress = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(emailAddress)) return UnregisteredEmailAddress
    val isReset =
        Users.resetPassword(emailAddress, env.getArgument("passwordResetCode"), env.getArgument("newPassword"))
    return if (isReset) null else InvalidPasswordResetCode
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
    val userIdList = env.getArgument<List<Int>>("idList").filter(Users::isExisting)
    GroupChatUsers.addUsers(chatId, userIdList)
    return Placeholder
}

fun removeGroupChatUsers(env: DataFetchingEnvironment): CannotLeaveChat? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("idList").toSet()
    try {
        GroupChatUsers.removeUsers(chatId, userIdList)
    } catch (_: IllegalArgumentException) {
        return CannotLeaveChat
    }
    return null
}

fun leaveGroupChat(env: DataFetchingEnvironment): LeaveGroupChatResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (chatId !in GroupChatUsers.readChatIdList(env.userId!!)) return InvalidChatId
    if (!GroupChatUsers.canUsersLeave(chatId, env.userId!!)) return CannotLeaveChat
    GroupChatUsers.removeUsers(chatId, env.userId!!)
    return null
}

fun makeGroupChatAdmins(env: DataFetchingEnvironment): Placeholder {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("idList").filter { isUserInChat(it, chatId) }
    GroupChatUsers.makeAdmins(chatId, userIdList)
    return Placeholder
}

fun createPollMessage(env: DataFetchingEnvironment): CreatePollMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val poll = try {
        env.parseArgument<PollInput>("poll")
    } catch (_: IllegalArgumentException) {
        return InvalidPoll
    }
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) return InvalidMessageId
    Messages.createPollMessage(env.userId!!, chatId, poll, contextMessageId)
    return null
}

fun setPollVote(env: DataFetchingEnvironment): SetPollVoteResult? {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId) || !PollMessages.isExisting(messageId)) return InvalidMessageId
    val option = env.getArgument<MessageText>("option")
    if (!PollMessages.hasOption(messageId, option)) return NonexistentOption
    val vote = env.getArgument<Boolean>("vote")
    PollMessages.setVote(env.userId!!, messageId, option, vote)
    return null
}

fun joinGroupChat(env: DataFetchingEnvironment): InvalidInviteCode? {
    env.verifyAuth()
    val inviteCode = env.getArgument<UUID>("inviteCode")
    if (!GroupChats.isExistentInviteCode(inviteCode)) return InvalidInviteCode
    GroupChatUsers.addUserViaInvite(env.userId!!, inviteCode)
    return null
}

fun joinPublicChat(env: DataFetchingEnvironment): InvalidChatId? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChats.isExistentPublicChat(chatId)) return InvalidChatId
    GroupChatUsers.addUsers(chatId, env.userId!!)
    return null
}

fun createGroupChatInviteMessage(env: DataFetchingEnvironment): CreateGroupChatInviteMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val invitedChatId = env.getArgument<Int>("invitedChatId")
    if (!GroupChats.isInvitable(invitedChatId)) return InvalidInvitedChat
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) return InvalidMessageId
    Messages.createGroupChatInviteMessage(env.userId!!, chatId, invitedChatId, contextMessageId)
    return null
}

fun setInvitability(env: DataFetchingEnvironment): InvalidChatId? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (GroupChats.isExistentPublicChat(chatId)) return InvalidChatId
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.setInvitability(chatId, env.getArgument("isInvitable"))
    return null
}

fun createActionMessage(env: DataFetchingEnvironment): CreateActionMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!isUserInChat(env.userId!!, chatId)) return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    val message = try {
        env.parseArgument<ActionMessageInput>("message")
    } catch (_: IllegalArgumentException) {
        return InvalidAction
    }
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (contextMessageId != null && contextMessageId !in Messages.readIdList(chatId)) return InvalidMessageId
    Messages.createActionMessage(env.userId!!, chatId, message, contextMessageId)
    return null
}

fun triggerAction(env: DataFetchingEnvironment): TriggerActionResult? {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId) || !ActionMessages.isExisting(messageId))
        return InvalidMessageId
    val action = env.getArgument<MessageText>("action")
    if (!ActionMessages.hasAction(messageId, action)) return InvalidAction
    ActionMessages.trigger(env.userId!!, messageId, action)
    return null
}
