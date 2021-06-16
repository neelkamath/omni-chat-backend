package com.neelkamath.omniChatBackend.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.MessageType
import com.neelkamath.omniChatBackend.db.deleteUser
import com.neelkamath.omniChatBackend.db.isUserInChat
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.*
import com.neelkamath.omniChatBackend.graphql.engine.UnauthorizedException
import com.neelkamath.omniChatBackend.graphql.engine.parseArgument
import com.neelkamath.omniChatBackend.graphql.engine.verifyAuth
import com.neelkamath.omniChatBackend.graphql.routing.*
import graphql.schema.DataFetchingEnvironment
import kotlinx.coroutines.runBlocking
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
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) return InvalidBroadcast
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (!Messages.isValidContext(env.userId!!, chatId, contextMessageId)) return InvalidMessageId
    Messages.createTextMessage(env.userId!!, chatId, env.getArgument("text"), contextMessageId)
    return null
}

@Suppress("DuplicatedCode")
fun forwardMessage(env: DataFetchingEnvironment): ForwardMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId)) return InvalidMessageId
    val isInvalidInvite = Messages.readType(messageId) == MessageType.GROUP_CHAT_INVITE &&
            chatId == GroupChatInviteMessages.read(messageId)
    if (isInvalidInvite || !isUserInChat(env.userId!!, chatId) || Messages.readChatId(messageId) == chatId)
        return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) return InvalidBroadcast
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (!Messages.isValidContext(env.userId!!, chatId, contextMessageId)) return InvalidMessageId
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
    val chatId =
        if (PrivateChats.isExisting(env.userId!!, invitedUserId)) PrivateChats.readChatId(invitedUserId, env.userId!!)
        else PrivateChats.create(env.userId!!, invitedUserId)
    return CreatedChatId(chatId)
}

fun deleteAccount(env: DataFetchingEnvironment): CannotDeleteAccount? {
    env.verifyAuth()
    if (!GroupChatUsers.canUserLeave(env.userId!!)) return CannotDeleteAccount
    runBlocking { deleteUser(env.userId!!) }
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
    val chatId = Messages.readChatId(messageId)
    if (!isUserInChat(env.userId!!, chatId) ||
        Messages.readSenderId(messageId) != env.userId!! ||
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
    if (wantsTakenEmailAddress(env.userId!!, update.emailAddress)) return EmailAddressTaken
    val emailAddress = Users.readEmailAddress(env.userId!!)
    if (update.emailAddress != null && update.emailAddress != emailAddress) {
        val code = Users.readEmailAddressVerificationCode(env.userId!!)
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
    wantedUsername != null && Users.readUsername(userId) != wantedUsername && Users.isUsernameTaken(wantedUsername)

private fun wantsTakenEmailAddress(userId: Int, wantedEmailAddress: String?): Boolean = wantedEmailAddress != null &&
        Users.readEmailAddress(userId) != wantedEmailAddress &&
        Users.isEmailAddressTaken(wantedEmailAddress)

fun emailEmailAddressVerification(env: DataFetchingEnvironment): EmailEmailAddressVerificationResult? {
    val address = env.getArgument<String>("emailAddress")
    if (!Users.isEmailAddressTaken(address)) return UnregisteredEmailAddress
    if (Users.hasVerifiedEmailAddress(address)) return EmailAddressVerified
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
    val userIdList = env.getArgument<List<Int>>("userIdList").filter(Users::isExisting)
    GroupChatUsers.addUsers(chatId, userIdList)
    return Placeholder
}

fun removeGroupChatUsers(env: DataFetchingEnvironment): CannotLeaveChat? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    val userIdList = env.getArgument<List<Int>>("userIdList").toSet()
    try {
        runBlocking { GroupChatUsers.removeUsers(chatId, userIdList) }
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
    runBlocking { GroupChatUsers.removeUsers(chatId, env.userId!!) }
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
    if (!Messages.isValidContext(env.userId!!, chatId, contextMessageId)) return InvalidMessageId
    Messages.createPollMessage(env.userId!!, chatId, poll, contextMessageId)
    return null
}

fun setPollVote(env: DataFetchingEnvironment): SetPollVoteResult? {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    if (!Messages.isVisible(env.userId!!, messageId) || !PollMessages.isExisting(messageId))
        return InvalidMessageId
    val option = env.getArgument<MessageText>("option")
    if (!PollMessageOptions.hasOption(messageId, option)) return NonexistingOption
    val vote = env.getArgument<Boolean>("vote")
    PollMessages.setVote(env.userId!!, messageId, option, vote)
    return null
}

fun joinGroupChat(env: DataFetchingEnvironment): InvalidInviteCode? {
    env.verifyAuth()
    val inviteCode = env.getArgument<UUID>("inviteCode")
    if (!GroupChats.isExistingInviteCode(inviteCode)) return InvalidInviteCode
    GroupChatUsers.addUserViaInviteCode(env.userId!!, inviteCode)
    return null
}

fun joinPublicChat(env: DataFetchingEnvironment): InvalidChatId? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (!GroupChats.isExistingPublicChat(chatId)) return InvalidChatId
    GroupChatUsers.addUsers(chatId, env.userId!!)
    return null
}

fun createGroupChatInviteMessage(env: DataFetchingEnvironment): CreateGroupChatInviteMessageResult? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    val invitedChatId = env.getArgument<Int>("invitedChatId")
    if (!isUserInChat(env.userId!!, chatId) || !isUserInChat(env.userId!!, invitedChatId) || chatId == invitedChatId)
        return InvalidChatId
    if (Messages.isInvalidBroadcast(env.userId!!, chatId)) throw UnauthorizedException
    if (!GroupChats.isInvitable(invitedChatId)) return InvalidInvitedChat
    val contextMessageId = env.getArgument<Int?>("contextMessageId")
    if (!Messages.isValidContext(env.userId!!, chatId, contextMessageId)) return InvalidMessageId
    Messages.createGroupChatInviteMessage(env.userId!!, chatId, invitedChatId, contextMessageId)
    return null
}

fun setPublicity(env: DataFetchingEnvironment): InvalidChatId? {
    env.verifyAuth()
    val chatId = env.getArgument<Int>("chatId")
    if (PrivateChats.isExisting(chatId) || GroupChats.isExistingPublicChat(chatId)) return InvalidChatId
    if (!GroupChatUsers.isAdmin(env.userId!!, chatId)) throw UnauthorizedException
    GroupChats.setPublicity(chatId, env.getArgument("isInvitable"))
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
    if (!Messages.isValidContext(env.userId!!, chatId, contextMessageId)) return InvalidMessageId
    Messages.createActionMessage(env.userId!!, chatId, message, contextMessageId)
    return null
}

fun triggerAction(env: DataFetchingEnvironment): Boolean {
    env.verifyAuth()
    val messageId = env.getArgument<Int>("messageId")
    val action = env.getArgument<MessageText>("action")
    if (!ActionMessages.isExisting(messageId) || !ActionMessages.isValidTrigger(env.userId!!, messageId, action))
        return false
    ActionMessages.trigger(env.userId!!, messageId, action)
    return true
}
