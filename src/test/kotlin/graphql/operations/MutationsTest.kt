package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.DbExtension
import com.neelkamath.omniChat.createVerifiedUsers
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.safelySubscribe
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.readPic
import com.neelkamath.omniChat.testingObjectMapper
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.*

private const val UNBLOCK_USER_QUERY = """
    mutation UnblockUser(${"$"}userId: Int!) {
        unblockUser(userId: ${"$"}userId)
    }
"""

private fun operateUnblockUser(userId: Int, blockedUserId: Int): GraphQlResponse =
    executeGraphQlViaEngine(UNBLOCK_USER_QUERY, mapOf("userId" to blockedUserId), userId)

private fun unblockUser(userId: Int, blockedUserId: Int): Placeholder {
    val data = operateUnblockUser(userId, blockedUserId).data!!["unblockUser"] as String
    return testingObjectMapper.convertValue(data)
}

private const val BLOCK_USER_QUERY = """
    mutation BlockUser(${"$"}userId: Int!) {
        blockUser(userId: ${"$"}userId)
    }
"""

private fun operateBlockUser(userId: Int, blockedUserId: Int): GraphQlResponse =
    executeGraphQlViaEngine(BLOCK_USER_QUERY, mapOf("userId" to blockedUserId), userId)

private fun blockUser(userId: Int, blockedUserId: Int): Placeholder {
    val data = operateBlockUser(userId, blockedUserId).data!!["blockUser"] as String
    return testingObjectMapper.convertValue(data)
}

private fun errBlockUser(userId: Int, blockedUserId: Int): String =
    operateBlockUser(userId, blockedUserId).errors!![0].message

private const val RESET_PASSWORD_QUERY = """
    mutation ResetPassword(${"$"}emailAddress: String!, ${"$"}passwordResetCode: Int!, ${"$"}newPassword: Password!) {
        resetPassword(
            emailAddress: ${"$"}emailAddress
            passwordResetCode: ${"$"}passwordResetCode
            newPassword: ${"$"}newPassword
        )
    }
"""

private fun operateResetPassword(
    emailAddress: String,
    passwordResetCode: Int,
    newPassword: Password,
): GraphQlResponse = executeGraphQlViaEngine(
    RESET_PASSWORD_QUERY,
    mapOf("emailAddress" to emailAddress, "passwordResetCode" to passwordResetCode, "newPassword" to newPassword.value),
)

fun resetPassword(emailAddress: String, passwordResetCode: Int, newPassword: Password): Boolean =
    operateResetPassword(emailAddress, passwordResetCode, newPassword).data!!["resetPassword"] as Boolean

fun errResetPassword(emailAddress: String, passwordResetCode: Int, newPassword: Password): String =
    operateResetPassword(emailAddress, passwordResetCode, newPassword).errors!![0].message

private const val VERIFY_EMAIL_ADDRESS_QUERY = """
    mutation VerifyEmailAddress(${"$"}emailAddress: String!, ${"$"}verificationCode: Int!) {
        verifyEmailAddress(emailAddress: ${"$"}emailAddress, verificationCode: ${"$"}verificationCode)
    }
"""

private fun operateVerifyEmailAddress(emailAddress: String, verificationCode: Int): GraphQlResponse =
    executeGraphQlViaEngine(
        VERIFY_EMAIL_ADDRESS_QUERY,
        mapOf("emailAddress" to emailAddress, "verificationCode" to verificationCode)
    )

fun verifyEmailAddress(emailAddress: String, verificationCode: Int): Boolean =
    operateVerifyEmailAddress(emailAddress, verificationCode).data!!["verifyEmailAddress"] as Boolean

fun errVerifyEmailAddress(emailAddress: String, verificationCode: Int): String =
    operateVerifyEmailAddress(emailAddress, verificationCode).errors!![0].message

private const val TRIGGER_ACTION_QUERY = """
    mutation TriggerAction(${"$"}messageId: Int!, ${"$"}action: MessageText!) {
        triggerAction(messageId: ${"$"}messageId, action: ${"$"}action)
    }
"""

private fun operateTriggerAction(userId: Int, messageId: Int, action: MessageText): GraphQlResponse =
    executeGraphQlViaEngine(TRIGGER_ACTION_QUERY, mapOf("messageId" to messageId, "action" to action), userId)

fun triggerAction(userId: Int, messageId: Int, action: MessageText): Placeholder {
    val data = operateTriggerAction(userId, messageId, action).data!!["triggerAction"] as String
    return testingObjectMapper.convertValue(data)
}

fun errTriggerAction(userId: Int, messageId: Int, action: MessageText): String =
    operateTriggerAction(userId, messageId, action).errors!![0].message

private const val CREATE_ACTION_MESSAGE_QUERY = """
    mutation CreateActionMessage(${"$"}chatId: Int!, ${"$"}message: ActionMessageInput!, ${"$"}contextMessageId: Int) {
        createActionMessage(chatId: ${"$"}chatId, message: ${"$"}message, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateCreateActionMessage(
    userId: Int,
    chatId: Int,
    message: ActionMessageInput,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_ACTION_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "message" to message, "contextMessageId" to contextMessageId),
    userId
)

fun createActionMessage(
    userId: Int,
    chatId: Int,
    message: ActionMessageInput,
    contextMessageId: Int? = null
): Placeholder {
    val data = operateCreateActionMessage(userId, chatId, message, contextMessageId)
        .data!!["createActionMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateActionMessage(
    userId: Int,
    chatId: Int,
    message: ActionMessageInput,
    contextMessageId: Int? = null
): String = operateCreateActionMessage(userId, chatId, message, contextMessageId).errors!![0].message

private const val FORWARD_MESSAGE_QUERY = """
    mutation ForwardMessage(${"$"}chatId: Int!, ${"$"}messageId: Int!, ${"$"}contextMessageId: Int) {
        forwardMessage(chatId: ${"$"}chatId, messageId: ${"$"}messageId, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateForwardMessage(
    userId: Int,
    chatId: Int,
    messageId: Int,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    FORWARD_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "messageId" to messageId, "contextMessageId" to contextMessageId),
    userId
)

fun forwardMessage(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int? = null): Placeholder {
    val data = operateForwardMessage(userId, chatId, messageId, contextMessageId).data!!["forwardMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errForwardMessage(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int? = null): String =
    operateForwardMessage(userId, chatId, messageId, contextMessageId).errors!![0].message

private const val REMOVE_GROUP_CHAT_USERS_QUERY = """
    mutation RemoveGroupChatUsers(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
        removeGroupChatUsers(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList)
    }
"""

private fun operateRemoveGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(
        REMOVE_GROUP_CHAT_USERS_QUERY,
        mapOf("chatId" to chatId, "userIdList" to userIdList),
        userId
    )

fun removeGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): Placeholder {
    val data = operateRemoveGroupChatUsers(userId, chatId, userIdList).data!!["removeGroupChatUsers"] as String
    return testingObjectMapper.convertValue(data)
}

fun errRemoveGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): String =
    operateRemoveGroupChatUsers(userId, chatId, userIdList).errors!![0].message

private const val SET_INVITABILITY_QUERY = """
    mutation SetInvitability(${"$"}chatId: Int!, ${"$"}isInvitable: Boolean!) {
        setInvitability(chatId: ${"$"}chatId, isInvitable: ${"$"}isInvitable)
    }
"""

private fun operateSetInvitability(userId: Int, chatId: Int, isInvitable: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_INVITABILITY_QUERY, mapOf("chatId" to chatId, "isInvitable" to isInvitable), userId)

fun setInvitability(userId: Int, chatId: Int, isInvitable: Boolean): Placeholder {
    val data = operateSetInvitability(userId, chatId, isInvitable).data!!["setInvitability"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetInvitability(userId: Int, chatId: Int, isInvitable: Boolean): String =
    operateSetInvitability(userId, chatId, isInvitable).errors!![0].message

private const val CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY = """
    mutation CreateGroupChatInviteMessage(${"$"}chatId: Int!, ${"$"}invitedChatId: Int!, ${"$"}contextMessageId: Int) {
        createGroupChatInviteMessage(
            chatId: ${"$"}chatId
            invitedChatId: ${"$"}invitedChatId
            contextMessageId: ${"$"}contextMessageId
        )
    }
"""

private fun operateCreateGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "invitedChatId" to invitedChatId, "contextMessageId" to contextMessageId),
    userId
)

fun createGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null
): Placeholder {
    val data = operateCreateGroupChatInviteMessage(userId, chatId, invitedChatId, contextMessageId)
        .data!!["createGroupChatInviteMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null
): String = operateCreateGroupChatInviteMessage(userId, chatId, invitedChatId, contextMessageId).errors!![0].message

private const val JOIN_GROUP_CHAT_QUERY = """
    mutation JoinGroupChat(${"$"}inviteCode: Uuid!) {
        joinGroupChat(inviteCode: ${"$"}inviteCode)
    }
"""

private fun operateJoinGroupChat(userId: Int, inviteCode: UUID): GraphQlResponse =
    executeGraphQlViaEngine(JOIN_GROUP_CHAT_QUERY, mapOf("inviteCode" to inviteCode), userId)

fun joinGroupChat(userId: Int, inviteCode: UUID): Placeholder {
    val data = operateJoinGroupChat(userId, inviteCode).data!!["joinGroupChat"] as String
    return testingObjectMapper.convertValue(data)
}

fun errJoinGroupChat(userId: Int, inviteCode: UUID): String =
    operateJoinGroupChat(userId, inviteCode).errors!![0].message

private const val CREATE_POLL_MESSAGE_QUERY = """
    mutation CreatePollMessage(${"$"}chatId: Int!, ${"$"}poll: PollInput!, ${"$"}contextMessageId: Int) {
        createPollMessage(chatId: ${"$"}chatId, poll: ${"$"}poll, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateCreatePollMessage(
    userId: Int,
    chatId: Int,
    poll: PollInput,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_POLL_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to contextMessageId),
    userId
)

fun createPollMessage(userId: Int, chatId: Int, poll: PollInput, contextMessageId: Int? = null): Placeholder {
    val data = operateCreatePollMessage(userId, chatId, poll, contextMessageId).data!!["createPollMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreatePollMessage(userId: Int, chatId: Int, poll: PollInput, contextMessageId: Int? = null): String =
    operateCreatePollMessage(userId, chatId, poll, contextMessageId).errors!![0].message

private const val SET_POLL_VOTE_QUERY = """
    mutation SetPollVote(${"$"}messageId: Int!, ${"$"}option: MessageText!, ${"$"}vote: Boolean!) {
        setPollVote(messageId: ${"$"}messageId, option: ${"$"}option, vote: ${"$"}vote)
    }
"""

private fun operateSetPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(
        SET_POLL_VOTE_QUERY,
        mapOf("messageId" to messageId, "option" to option, "vote" to vote),
        userId
    )

fun setPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): Placeholder {
    val data = operateSetPollVote(userId, messageId, option, vote).data!!["setPollVote"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): String =
    operateSetPollVote(userId, messageId, option, vote).errors!![0].message

private const val SET_BROADCAST_QUERY = """
    mutation SetBroadcast(${"$"}chatId: Int!, ${"$"}isBroadcast: Boolean!) {
        setBroadcast(chatId: ${"$"}chatId, isBroadcast: ${"$"}isBroadcast)
    }
"""

private fun operateSetBroadcast(userId: Int, chatId: Int, isBroadcast: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_BROADCAST_QUERY, mapOf("chatId" to chatId, "isBroadcast" to isBroadcast), userId)

fun setBroadcast(userId: Int, chatId: Int, isBroadcast: Boolean): Placeholder {
    val data = operateSetBroadcast(userId, chatId, isBroadcast).data!!["setBroadcast"] as String
    return testingObjectMapper.convertValue(data)
}

private const val MAKE_GROUP_CHAT_ADMINS_QUERY = """
    mutation MakeGroupChatAdmins(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
        makeGroupChatAdmins(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList)
    }
"""

private fun operateMakeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(MAKE_GROUP_CHAT_ADMINS_QUERY, mapOf("chatId" to chatId, "userIdList" to userIdList), userId)

fun makeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): Placeholder {
    val data = operateMakeGroupChatAdmins(userId, chatId, userIdList).data!!["makeGroupChatAdmins"] as String
    return testingObjectMapper.convertValue(data)
}

fun errMakeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): String =
    operateMakeGroupChatAdmins(userId, chatId, userIdList).errors!![0].message

private const val ADD_GROUP_CHAT_USERS_QUERY = """
    mutation AddGroupChatUsers(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
        addGroupChatUsers(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList)
    }
"""

private fun operateAddGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(ADD_GROUP_CHAT_USERS_QUERY, mapOf("chatId" to chatId, "userIdList" to userIdList), userId)

fun addGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): Placeholder {
    val data = operateAddGroupChatUsers(userId, chatId, userIdList).data!!["addGroupChatUsers"] as String
    return testingObjectMapper.convertValue(data)
}

fun errAddGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): String =
    operateAddGroupChatUsers(userId, chatId, userIdList).errors!![0].message

private const val UPDATE_GROUP_CHAT_DESCRIPTION_QUERY = """
    mutation UpdateGroupChatDescription(${"$"}chatId: Int!, ${"$"}description: GroupChatDescription!) {
        updateGroupChatDescription(chatId: ${"$"}chatId, description: ${"$"}description)
    }
"""

private fun operateUpdateGroupChatDescription(
    userId: Int,
    chatId: Int,
    description: GroupChatDescription
): GraphQlResponse = executeGraphQlViaEngine(
    UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
    mapOf("chatId" to chatId, "description" to description.value),
    userId
)

fun updateGroupChatDescription(userId: Int, chatId: Int, description: GroupChatDescription): Placeholder {
    val data =
        operateUpdateGroupChatDescription(userId, chatId, description).data!!["updateGroupChatDescription"] as String
    return testingObjectMapper.convertValue(data)
}

const val UPDATE_GROUP_CHAT_TITLE_QUERY = """
    mutation UpdateGroupChatTitle(${"$"}chatId: Int!, ${"$"}title: GroupChatTitle!) {
        updateGroupChatTitle(chatId: ${"$"}chatId, title: ${"$"}title)
    }
"""

private fun operateUpdateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_GROUP_CHAT_TITLE_QUERY, mapOf("chatId" to chatId, "title" to title.value), userId)

fun updateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): Placeholder {
    val data = operateUpdateGroupChatTitle(userId, chatId, title).data!!["updateGroupChatTitle"] as String
    return testingObjectMapper.convertValue(data)
}

private const val DELETE_STAR_QUERY = """
    mutation DeleteStar(${"$"}messageId: Int!) {
        deleteStar(messageId: ${"$"}messageId)
    }
"""

private fun operateDeleteStar(userId: Int, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_STAR_QUERY, mapOf("messageId" to messageId), userId)

fun deleteStar(userId: Int, messageId: Int): Placeholder {
    val data = operateDeleteStar(userId, messageId).data!!["deleteStar"] as String
    return testingObjectMapper.convertValue(data)
}

private const val STAR_QUERY = """
    mutation Star(${"$"}messageId: Int!) {
        star(messageId: ${"$"}messageId)
    }
"""

private fun operateStar(userId: Int, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(STAR_QUERY, mapOf("messageId" to messageId), userId)

fun star(userId: Int, messageId: Int): Placeholder {
    val data = operateStar(userId, messageId).data!!["star"] as String
    return testingObjectMapper.convertValue(data)
}

fun errStar(userId: Int, messageId: Int): String = operateStar(userId, messageId).errors!![0].message

private const val SET_ONLINE_QUERY = """
    mutation SetOnline(${"$"}isOnline: Boolean!) {
        setOnline(isOnline: ${"$"}isOnline)
    }
"""

private fun operateSetOnline(userId: Int, isOnline: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_ONLINE_QUERY, mapOf("isOnline" to isOnline), userId)

fun setOnline(userId: Int, isOnline: Boolean): Placeholder {
    val data = operateSetOnline(userId, isOnline).data!!["setOnline"] as String
    return testingObjectMapper.convertValue(data)
}

private const val SET_TYPING_QUERY = """
    mutation SetTyping(${"$"}chatId: Int!, ${"$"}isTyping: Boolean!) {
        setTyping(chatId: ${"$"}chatId, isTyping: ${"$"}isTyping)
    }
"""

private fun operateSetTyping(userId: Int, chatId: Int, isTyping: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_TYPING_QUERY, mapOf("chatId" to chatId, "isTyping" to isTyping), userId)

fun setTyping(userId: Int, chatId: Int, isTyping: Boolean): Placeholder {
    val data = operateSetTyping(userId, chatId, isTyping).data!!["setTyping"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetTyping(userId: Int, chatId: Int, isTyping: Boolean): String =
    operateSetTyping(userId, chatId, isTyping).errors!![0].message

private const val DELETE_GROUP_CHAT_PIC_QUERY = """
    mutation DeleteGroupChatPic(${"$"}chatId: Int!) {
        deleteGroupChatPic(chatId: ${"$"}chatId)
    }
"""

private fun operateDeleteGroupChatPic(userId: Int, chatId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_GROUP_CHAT_PIC_QUERY, mapOf("chatId" to chatId), userId)

fun deleteGroupChatPic(userId: Int, chatId: Int): Placeholder {
    val data = operateDeleteGroupChatPic(userId, chatId).data!!["deleteGroupChatPic"] as String
    return testingObjectMapper.convertValue(data)
}

private const val DELETE_PROFILE_PIC_QUERY = """
    mutation DeleteProfilePic {
        deleteProfilePic
    }
"""

private fun operateDeleteProfilePic(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PROFILE_PIC_QUERY, userId = userId)

fun deleteProfilePic(userId: Int): Placeholder {
    val data = operateDeleteProfilePic(userId).data!!["deleteProfilePic"] as String
    return testingObjectMapper.convertValue(data)
}

private const val CREATE_ACCOUNTS_QUERY = """
    mutation CreateAccount(${"$"}account: AccountInput!) {
        createAccount(account: ${"$"}account)
    }
"""

private fun operateCreateAccount(account: AccountInput): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_ACCOUNTS_QUERY, mapOf("account" to account))

fun createAccount(account: AccountInput): Placeholder {
    val data = operateCreateAccount(account).data!!["createAccount"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateAccount(account: AccountInput): String = operateCreateAccount(account).errors!![0].message

private const val CREATE_CONTACTS_QUERY = """
    mutation CreateContacts(${"$"}userIdList: [Int!]!) {
        createContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateCreateContacts(userId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_CONTACTS_QUERY, mapOf("userIdList" to userIdList), userId)

fun createContacts(userId: Int, userIdList: List<Int>): Placeholder {
    val data = operateCreateContacts(userId, userIdList).data!!["createContacts"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateContacts(userId: Int, userIdList: List<Int>): String =
    operateCreateContacts(userId, userIdList).errors!![0].message

private const val CREATE_GROUP_CHAT_QUERY = """
    mutation CreateGroupChat(${"$"}chat: GroupChatInput!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

private fun operateCreateGroupChat(userId: Int, chat: GroupChatInput): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), userId)

fun errCreateGroupChat(userId: Int, chat: GroupChatInput): String =
    operateCreateGroupChat(userId, chat).errors!![0].message

private const val CREATE_MESSAGE_QUERY = """
    mutation CreateTextMessage(${"$"}chatId: Int!, ${"$"}text: MessageText!, ${"$"}contextMessageId: Int) {
        createTextMessage(chatId: ${"$"}chatId, text: ${"$"}text, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateCreateTextMessage(
    userId: Int,
    chatId: Int,
    text: MessageText,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "text" to text, "contextMessageId" to contextMessageId),
    userId
)

fun createTextMessage(userId: Int, chatId: Int, text: MessageText, contextMessageId: Int? = null): Placeholder {
    val data = operateCreateTextMessage(userId, chatId, text, contextMessageId).data!!["createTextMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateTextMessage(userId: Int, chatId: Int, text: MessageText, contextMessageId: Int? = null): String =
    operateCreateTextMessage(userId, chatId, text, contextMessageId).errors!![0].message

private const val CREATE_PRIVATE_CHAT_QUERY = """
    mutation CreatePrivateChat(${"$"}userId: Int!) {
        createPrivateChat(userId: ${"$"}userId)
    }
"""

private fun operateCreatePrivateChat(userId: Int, otherUserId: Int): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_PRIVATE_CHAT_QUERY, mapOf("userId" to otherUserId), userId)

fun createPrivateChat(userId: Int, otherUserId: Int): Int =
    operateCreatePrivateChat(userId, otherUserId).data!!["createPrivateChat"] as Int

fun errCreatePrivateChat(userId: Int, otherUserId: Int): String =
    operateCreatePrivateChat(userId, otherUserId).errors!![0].message

private const val CREATE_STATUS_QUERY = """
    mutation CreateStatus(${"$"}messageId: Int!, ${"$"}status: MessageStatus!) {
        createStatus(messageId: ${"$"}messageId, status: ${"$"}status)
    }
"""

private fun operateCreateStatus(userId: Int, messageId: Int, status: MessageStatus): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_STATUS_QUERY, mapOf("messageId" to messageId, "status" to status), userId)

fun createStatus(userId: Int, messageId: Int, status: MessageStatus): Placeholder {
    val data = operateCreateStatus(userId, messageId, status).data!!["createStatus"] as String
    return testingObjectMapper.convertValue(data)
}

fun errCreateStatus(userId: Int, messageId: Int, status: MessageStatus): String =
    operateCreateStatus(userId, messageId, status).errors!![0].message

private const val DELETE_ACCOUNT_QUERY = """
    mutation DeleteAccount {
        deleteAccount
    }
"""

private fun operateDeleteAccount(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_ACCOUNT_QUERY, userId = userId)

fun deleteAccount(userId: Int): Placeholder {
    val data = operateDeleteAccount(userId).data!!["deleteAccount"] as String
    return testingObjectMapper.convertValue(data)
}

fun errDeleteAccount(userId: Int): String = operateDeleteAccount(userId).errors!![0].message

private const val DELETE_CONTACTS_QUERY = """
    mutation DeleteContacts(${"$"}userIdList: [Int!]!) {
        deleteContacts(userIdList: ${"$"}userIdList)
    }
"""

private fun operateDeleteContacts(userId: Int, userIdList: List<Int>): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_CONTACTS_QUERY, mapOf("userIdList" to userIdList), userId)

fun deleteContacts(userId: Int, userIdList: List<Int>): Placeholder {
    val data = operateDeleteContacts(userId, userIdList).data!!["deleteContacts"] as String
    return testingObjectMapper.convertValue(data)
}

private const val DELETE_MESSAGE_QUERY = """
    mutation DeleteMessage(${"$"}id: Int!) {
        deleteMessage(id: ${"$"}id)
    }
"""

private fun operateDeleteMessage(userId: Int, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_MESSAGE_QUERY, mapOf("id" to messageId), userId)

fun deleteMessage(userId: Int, messageId: Int): Placeholder {
    val data = operateDeleteMessage(userId, messageId).data!!["deleteMessage"] as String
    return testingObjectMapper.convertValue(data)
}

fun errDeleteMessage(userId: Int, messageId: Int): String =
    operateDeleteMessage(userId, messageId).errors!![0].message

private const val DELETE_PRIVATE_CHAT_QUERY = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId)
    }
"""

private fun operateDeletePrivateChat(userId: Int, chatId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PRIVATE_CHAT_QUERY, mapOf("chatId" to chatId), userId)

fun deletePrivateChat(userId: Int, chatId: Int): Placeholder {
    val data = operateDeletePrivateChat(userId, chatId).data!!["deletePrivateChat"] as String
    return testingObjectMapper.convertValue(data)
}

fun errDeletePrivateChat(userId: Int, chatId: Int): String =
    operateDeletePrivateChat(userId, chatId).errors!![0].message

private const val EMAIL_PASSWORD_RESET_CODE_QUERY = """
    mutation EmailPasswordResetCode(${"$"}emailAddress: String!) {
        emailPasswordResetCode(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateEmailPasswordResetCode(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(EMAIL_PASSWORD_RESET_CODE_QUERY, mapOf("emailAddress" to emailAddress))

fun emailPasswordResetCode(emailAddress: String): Placeholder {
    val data = operateEmailPasswordResetCode(emailAddress).data!!["emailPasswordResetCode"] as String
    return testingObjectMapper.convertValue(data)
}

fun errEmailPasswordResetCode(emailAddress: String): String =
    operateEmailPasswordResetCode(emailAddress).errors!![0].message

private const val EMAIL_EMAIL_ADDRESS_VERIFICATION_QUERY = """
    mutation EmailEmailAddressVerification(${"$"}emailAddress: String!) {
        emailEmailAddressVerification(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateEmailEmailAddressVerification(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(EMAIL_EMAIL_ADDRESS_VERIFICATION_QUERY, mapOf("emailAddress" to emailAddress))

fun emailEmailAddressVerification(emailAddress: String): Placeholder {
    val data = operateEmailEmailAddressVerification(emailAddress).data!!["emailEmailAddressVerification"] as String
    return testingObjectMapper.convertValue(data)
}

fun errEmailEmailAddressVerification(emailAddress: String): String =
    operateEmailEmailAddressVerification(emailAddress).errors!![0].message

private const val UPDATE_ACCOUNT_QUERY = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update)
    }
"""

private fun operateUpdateAccount(userId: Int, update: AccountUpdate): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_ACCOUNT_QUERY, mapOf("update" to update), userId)

fun updateAccount(userId: Int, update: AccountUpdate): Placeholder {
    val data = operateUpdateAccount(userId, update).data!!["updateAccount"] as String
    return testingObjectMapper.convertValue(data)
}

fun errUpdateAccount(userId: Int, update: AccountUpdate): String =
    operateUpdateAccount(userId, update).errors!![0].message

@ExtendWith(DbExtension::class)
class MutationsTest {
    @Nested
    inner class UnblockUser {
        @Test
        fun `The user must be unblocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            unblockUser(blockerId, blockedId)
            assertEquals(0, BlockedUsers.count())
        }
    }

    @Nested
    inner class BlockUser {
        @Test
        fun `The user must be blocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            blockUser(blockerId, blockedId)
            val userId = BlockedUsers.read(blockerId).edges[0].node.id
            assertEquals(blockedId, userId)
        }

        @Test
        fun `A nonexistent user mustn't be blocked`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidUserIdException.message, errBlockUser(userId, -1))
        }
    }

    @Nested
    inner class ResetPassword {
        @Test
        fun `The password must be reset`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val user = Users.read(account.username)
            val password = Password("new")
            assertTrue(resetPassword(user.emailAddress, user.passwordResetCode, password))
            val login = Login(account.username, password)
            assertTrue(Users.isValidLogin(login))
        }

        @Test
        fun `Using an invalid code mustn't reset the password`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val password = Password("new")
            assertFalse(resetPassword(account.emailAddress, 123, password))
            val login = Login(account.username, password)
            assertFalse(Users.isValidLogin(login))
        }

        @Test
        fun `Resetting the password for an unregistered email address must fail`(): Unit = assertEquals(
            UnregisteredEmailAddressException.message,
            errResetPassword("john@example.com", 123, Password("new"))
        )
    }

    @Nested
    inner class VerifyEmailAddress {
        @Test
        fun `The email address must get verified`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            val user = Users.read(account.username)
            assertTrue(verifyEmailAddress(user.emailAddress, user.emailAddressVerificationCode))
            assertTrue(Users.read(account.username).hasVerifiedEmailAddress)
        }

        @Test
        fun `Using an invalid code mustn't verify the email address`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            assertFalse(verifyEmailAddress(account.emailAddress, 123))
            assertFalse(Users.read(account.username).hasVerifiedEmailAddress)
        }

        @Test
        fun `Attempting to verify an email address which isn't associated with an account must fail`() {
            assertEquals(UnregisteredEmailAddressException.message, errVerifyEmailAddress("john.doe@example.com", 123))
        }
    }

    @Nested
    inner class TriggerAction {
        @Test
        fun `The action must be triggered`(): Unit = runBlocking {
            val admin = createVerifiedUsers(1)[0].info
            val chatId = GroupChats.create(listOf(admin.id))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                admin.id,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No")))
            )
            val subscriber = messagesNotifier.safelySubscribe(admin.id)
            triggerAction(admin.id, messageId, action)
            awaitBrokering()
            subscriber.assertValue(TriggeredAction(messageId, action, admin))
        }

        @Test
        fun `Triggering a message which isn't an action message must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId, MessageText("t"))
            assertEquals(
                InvalidMessageIdException.message,
                errTriggerAction(adminId, messageId, MessageText("action"))
            )
        }

        @Test
        fun `Triggering a nonexistent action must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No")))
            )
            assertEquals(
                InvalidActionException.message,
                errTriggerAction(adminId, messageId, MessageText("action"))
            )
        }
    }

    @Nested
    inner class CreateActionMessage {
        @Test
        fun `The message must be created with the context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId, MessageText("t"))
            val message = ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No")))
            createActionMessage(adminId, chatId, message, contextMessageId)
            val node = Messages.readGroupChatConnection(chatId).edges.last().node
            assertEquals(node.context.id, contextMessageId)
            assertEquals(message.toActionableMessage(), ActionMessages.read(node.messageId))
        }

        @Test
        fun `A non-admin mustn't be allowed to message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = executeGraphQlViaHttp(
                CREATE_ACTION_MESSAGE_QUERY,
                mapOf(
                    "chatId" to chatId,
                    "message" to ActionMessageInput(
                        MessageText("Do you code?"),
                        listOf(MessageText("Yes"), MessageText("No"))
                    )
                ),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Messaging in a chat the user isn't in must fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val response = errCreateActionMessage(
                userId,
                chatId = 1,
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No")))
            )
            assertEquals(InvalidChatIdException.message, response)
        }

        @Test
        fun `Supplying an invalid action message must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val error = executeGraphQlViaEngine(
                CREATE_ACTION_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "message" to mapOf("text" to "Do you code?", "actions" to listOf<String>())),
                adminId
            ).errors!![0].message
            assertEquals(InvalidActionException.message, error)
        }

        @Test
        fun `Using a nonexistent context message ID must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val error = errCreateActionMessage(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No"))),
                contextMessageId = 1
            )
            assertEquals(InvalidMessageIdException.message, error)
        }
    }

    @Nested
    inner class ForwardMessage {
        @Test
        fun `The message must be forwarded with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val messageId = Messages.message(adminId, chat1Id)
            val contextMessageId = Messages.message(adminId, chat2Id)
            forwardMessage(adminId, chat2Id, messageId, contextMessageId)
            val node = Messages.readGroupChat(chat2Id).last().node
            assertEquals(contextMessageId, node.context.id)
            assertTrue(node.isForwarded)
        }

        @Test
        fun `A non-admin mustn't be allowed to forward a message to a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val messageId = Messages.message(admin.info.id, chatId)
            val response = executeGraphQlViaHttp(
                FORWARD_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "messageId" to messageId),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Messaging in a chat the user isn't in must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            assertEquals(InvalidChatIdException.message, errForwardMessage(admin1Id, chat2Id, messageId))
        }

        @Test
        fun `Forwarding a nonexistent message must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(InvalidMessageIdException.message, errForwardMessage(adminId, chatId, messageId = 1))
        }

        @Test
        fun `Using an invalid context message must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val response = errForwardMessage(adminId, chatId, messageId, contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Forwarding a message the user can't see must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            assertEquals(InvalidMessageIdException.message, errForwardMessage(admin2Id, chat2Id, messageId))
        }
    }

    @Nested
    inner class SetInvitability {
        @Test
        fun `The chat's invitability must be updated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            setInvitability(adminId, chatId, isInvitable = true)
            assertEquals(GroupChatPublicity.INVITABLE, GroupChats.readChat(chatId).publicity)
        }

        @Test
        fun `Updating a public chat must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertEquals(InvalidChatIdException.message, errSetInvitability(adminId, chatId, isInvitable = true))
        }

        @Test
        fun `An error must be returned when a non-admin updates the invitability`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                SET_INVITABILITY_QUERY,
                mapOf("chatId" to chatId, "isInvitable" to true),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class CreateGroupChatInviteMessage {
        @Test
        fun `A message must be created with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2)
                .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val contextMessageId = Messages.message(adminId, chatId, MessageText("t"))
            createGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId)
            assertEquals(1, GroupChatInviteMessages.count())
        }

        @Test
        fun `Messaging in a broadcast chat must fail`() {
            val (admin1, admin2) = createVerifiedUsers(2)
            val chatId = GroupChats.create(
                adminIdList = listOf(admin1.info.id),
                userIdList = listOf(admin2.info.id),
                isBroadcast = true
            )
            val invitedChatId = GroupChats.create(listOf(admin2.info.id), publicity = GroupChatPublicity.INVITABLE)
            val response = executeGraphQlViaHttp(
                CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "invitedChatId" to invitedChatId),
                admin2.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Creating a message in a chat the user isn't in must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val invitedChatId = GroupChats.create(listOf(adminId))
            val response = errCreateGroupChatInviteMessage(adminId, chatId = 1, invitedChatId = invitedChatId)
            assertEquals(InvalidChatIdException.message, response)
        }

        @Test
        fun `Inviting users to a private chat must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminIdList = listOf(user1Id))
            val invitedChatId = PrivateChats.create(user1Id, user2Id)
            val response = errCreateGroupChatInviteMessage(user1Id, chatId, invitedChatId)
            assertEquals(InvalidInvitedChatException.message, response)
        }

        @Test
        fun `Inviting users to a group chat with invites turned off must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val response = errCreateGroupChatInviteMessage(adminId, chatId, invitedChatId)
            assertEquals(InvalidInvitedChatException.message, response)
        }

        @Test
        fun `Using an invalid content message must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2)
                .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val response = errCreateGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }
    }

    @Nested
    inner class JoinGroupChat {
        @Test
        fun `An invite code must be used to join the chat, even if the chat has already been joined`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            repeat(2) { joinGroupChat(userId, GroupChats.readInviteCode(chatId)) }
            assertEquals(setOf(adminId, userId), GroupChatUsers.readUserIdList(chatId).toSet())
        }

        @Test
        fun `Using an invalid invite code must fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidInviteCodeException.message, errJoinGroupChat(userId, inviteCode = UUID.randomUUID()))
        }
    }

    @Nested
    inner class CreatePollMessage {
        @Test
        fun `A poll message must be created with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            createPollMessage(adminId, chatId, poll, contextMessageId)
            val message = Messages.readGroupChat(chatId, userId = adminId).last().node
            assertEquals(contextMessageId, message.context.id)
            val options = poll.options.map { PollOption(it, votes = listOf()) }
            assertEquals(Poll(poll.title, options), PollMessages.read(message.messageId))
        }

        @Test
        fun `Messaging a poll in a chat the user isn't in must fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            assertEquals(InvalidChatIdException.message, errCreatePollMessage(userId, chatId = 1, poll = poll))
        }

        @Test
        fun `Creating a poll in response to a nonexistent message must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val response = errCreatePollMessage(adminId, chatId, poll, contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Using an invalid poll must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = mapOf("title" to "Title", "options" to listOf("option"))
            val response = executeGraphQlViaEngine(
                CREATE_POLL_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to null),
                adminId
            ).errors!![0].message
            assertEquals(InvalidPollException.message, response)
        }
    }

    @Nested
    inner class SetPollVote {
        @Test
        fun `The user's vote must be updated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val option = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(option, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            setPollVote(adminId, messageId, option, vote = true)
            assertEquals(listOf(adminId), PollMessages.read(messageId).options.first { it.option == option }.votes)
        }

        @Test
        fun `Voting on a message which isn't a poll must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId, MessageText("t"))
            val response = errSetPollVote(adminId, messageId, MessageText("option"), vote = true)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Voting on a nonexistent poll must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val response = errSetPollVote(adminId, messageId = 1, option = MessageText("option"), vote = true)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Voting for a nonexistent option must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val response = errSetPollVote(adminId, messageId, MessageText("nonexistent option"), vote = true)
            assertEquals(NonexistentOptionException.message, response)
        }
    }

    @Nested
    inner class SetBroadcast {
        @Test
        fun `Only an admin must be allowed to set the broadcast status`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                SET_BROADCAST_QUERY,
                mapOf("chatId" to chatId, "isBroadcast" to true),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `The broadcast status must be updated`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val isBroadcast = true
            setBroadcast(adminId, chatId, isBroadcast)
            assertEquals(isBroadcast, GroupChats.readChat(chatId, userId = adminId).isBroadcast)
        }
    }

    @Nested
    inner class MakeGroupChatAdmins {
        @Test
        fun `The users must be made admins`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            makeGroupChatAdmins(adminId, chatId, listOf(userId))
            assertEquals(setOf(adminId, userId), GroupChatUsers.readAdminIdList(chatId).toSet())
        }

        @Test
        fun `Making a user who isn't in the chat an admin must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(InvalidUserIdException.message, errMakeGroupChatAdmins(adminId, chatId, listOf(userId)))
        }

        @Test
        fun `A non-admin mustn't be allowed to make users admins`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                MAKE_GROUP_CHAT_ADMINS_QUERY,
                mapOf("chatId" to chatId, "userIdList" to listOf<Int>()),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class AddGroupChatUsers {
        @Test
        fun `Users must be added to the chat while ignoring duplicates and existing users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            addGroupChatUsers(adminId, chatId, listOf(adminId, userId, userId))
            assertEquals(listOf(adminId, userId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `An exception must be thrown when adding a nonexistent user`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val invalidUserId = -1
            assertEquals(InvalidUserIdException.message, errAddGroupChatUsers(adminId, chatId, listOf(invalidUserId)))
        }

        @Test
        fun `A non-admin mustn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                ADD_GROUP_CHAT_USERS_QUERY,
                mapOf("chatId" to chatId, "userIdList" to listOf<Int>()),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class RemoveGroupChatUsers {
        @Test
        fun `The admin must be allowed to remove themselves along with non-admins if they aren't the last admin`() {
            val (admin1Id, admin2Id, userId) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id, admin2Id), listOf(userId))
            removeGroupChatUsers(admin1Id, chatId, listOf(admin1Id, userId))
            assertEquals(listOf(admin2Id), GroupChats.readChat(chatId).users.edges.map { it.node.id })
        }

        @Test
        fun `Removing the last admin must be allowed if there won't be any remaining users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            removeGroupChatUsers(adminId, chatId, listOf(adminId, userId))
            assertEquals(0, GroupChats.count())
        }

        @Test
        fun `Removing the last admin mustn't be allowed if there are other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertEquals(InvalidUserIdException.message, errRemoveGroupChatUsers(adminId, chatId, listOf(adminId)))
        }

        @Test
        fun `Removing a nonexistent user must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertEquals(InvalidUserIdException.message, errRemoveGroupChatUsers(adminId, chatId, listOf(-1)))
        }
    }

    @Nested
    inner class UpdateGroupChatDescription {
        @Test
        fun `The admin must update the description`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val description = GroupChatDescription("New description.")
            updateGroupChatDescription(adminId, chatId, description)
            assertEquals(description, GroupChats.readChat(chatId, userId = adminId).description)
        }

        @Test
        fun `A non-admin mustn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
                mapOf("chatId" to chatId, "description" to "d"),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class UpdateGroupChatTitle {
        @Test
        fun `The admin must update the title`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val title = GroupChatTitle("New Title")
            updateGroupChatTitle(adminId, chatId, title)
            assertEquals(title, GroupChats.readChat(chatId, userId = adminId).title)
        }

        @Test
        fun `A non-admin mustn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_TITLE_QUERY,
                mapOf("chatId" to chatId, "title" to "T"),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class DeleteStar {
        @Test
        fun `A message must be starred`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            deleteStar(adminId, messageId)
            assertFalse(Stargazers.hasStar(adminId, messageId))
        }
    }

    @Nested
    inner class Star {
        @Test
        fun `A message must be starred`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            star(adminId, messageId)
            assertEquals(listOf(messageId), Stargazers.read(adminId))
        }

        @Test
        fun `Starring a message from a chat the user isn't in must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            val messageId = Messages.message(admin1Id, chatId)
            assertEquals(InvalidMessageIdException.message, errStar(admin2Id, messageId))
        }
    }

    @Nested
    inner class SetOnline {
        private fun assertOnlineStatus(isOnline: Boolean) {
            val userId = createVerifiedUsers(1)[0].info.id
            setOnline(userId, isOnline)
            assertEquals(isOnline, Users.read(userId).isOnline)
        }

        @Test
        fun `The user's online status must be set to true`() {
            assertOnlineStatus(true)
        }

        @Test
        fun `The user's online status must be set to false`() {
            assertOnlineStatus(false)
        }
    }

    @Nested
    inner class SetTyping {
        private fun assertTypingStatus(isTyping: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            setTyping(adminId, chatId, isTyping)
            assertEquals(isTyping, TypingStatuses.read(chatId, adminId))
        }

        @Test
        fun `The user's typing status must be set to true`() {
            assertTypingStatus(isTyping = true)
        }

        @Test
        fun `The user's typing status must be set to false`() {
            assertTypingStatus(isTyping = false)
        }

        @Test
        fun `Setting the typing status in a chat the user isn't in must fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidChatIdException.message, errSetTyping(userId, chatId = 1, isTyping = true))
        }
    }

    @Nested
    inner class DeleteGroupChatPic {
        @Test
        fun `Deleting the pic must remove it`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            GroupChats.updatePic(chatId, readPic("76px×57px.jpg"))
            deleteGroupChatPic(adminId, chatId)
            assertNull(GroupChats.readPic(chatId))
        }

        @Test
        fun `An exception must be thrown when a non-admin updates the pic`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                DELETE_GROUP_CHAT_PIC_QUERY,
                mapOf("chatId" to chatId),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class DeleteProfilePic {
        @Test
        fun `The user's profile pic must be deleted`() {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.updatePic(userId, readPic("76px×57px.jpg"))
            deleteProfilePic(userId)
            assertNull(Users.read(userId).pic)
        }
    }

    @Nested
    inner class CreateAccount {
        @Test
        fun `Creating an account must save it to the auth system, and the DB`() {
            val account = AccountInput(Username("u"), Password("p"), "username@example.com")
            createAccount(account)
            with(Users.read(account.username)) {
                assertEquals(account.username, username)
                assertEquals(account.emailAddress, emailAddress)
            }
            assertEquals(1, Users.count())
        }

        @Test
        fun `An account with a taken username mustn't be created`() {
            val account = AccountInput(Username("u"), Password("p"), "username@example.com")
            createAccount(account)
            assertEquals(UsernameTakenException.message, errCreateAccount(account))
        }

        @Test
        fun `An account with a taken email mustn't be created`() {
            val address = "username@example.com"
            val account = AccountInput(Username("username1"), Password("p"), address)
            createAccount(account)
            val duplicateAccount = AccountInput(Username("username2"), Password("p"), address)
            assertEquals(EmailAddressTakenException.message, errCreateAccount(duplicateAccount))
        }

        @Test
        fun `An account with a disallowed email address domain mustn't be created`() {
            val response = errCreateAccount(AccountInput(Username("u"), Password("p"), "bob@outlook.com"))
            assertEquals(InvalidDomainException.message, response)
        }
    }

    @Nested
    inner class CreateContacts {
        @Test
        fun `Trying to save the user's own contact must be ignored`() {
            val (ownerId, userId) = createVerifiedUsers(2).map { it.info.id }
            createContacts(ownerId, listOf(ownerId, userId))
            assertEquals(listOf(userId), Contacts.readIdList(ownerId))
        }

        @Test
        fun `If one of the contacts to be saved is invalid, then none of them must be saved`() {
            val (ownerId, userId) = createVerifiedUsers(2).map { it.info.id }
            val contacts = listOf(userId, -1)
            assertEquals(InvalidContactException.message, errCreateContacts(ownerId, contacts))
            assertTrue(Contacts.readIdList(ownerId).isEmpty())
        }
    }

    @Nested
    inner class CreateGroupChat {
        @Test
        fun `A group chat must be created automatically including the creator as a user and admin`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf(user1Id, user2Id),
                "adminIdList" to listOf<Int>(),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE
            )
            val chatId = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), adminId)
                .data!!["createGroupChat"] as Int
            val chats = GroupChats.readUserChats(adminId)
            assertEquals(1, chats.size)
            assertEquals(chatId, chats[0].id)
            val participants = chats[0].users.edges.map { it.node.id }.toSet()
            assertEquals(setOf(adminId, user1Id, user2Id), participants)
            assertEquals(listOf(adminId), chats[0].adminIdList)
        }

        @Test
        fun `A group chat mustn't be created when supplied with an invalid user ID`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val invalidUserId = -1
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(userId, invalidUserId),
                adminIdList = listOf(userId),
                isBroadcast = false,
                publicity = GroupChatPublicity.NOT_INVITABLE
            )
            assertEquals(InvalidUserIdException.message, errCreateGroupChat(userId, chat))
        }

        @Test
        fun `A group chat mustn't be created if the admin ID list isn't a subset of the user ID list`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf<Int>(),
                "adminIdList" to listOf(user2Id),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE
            )
            val response = executeGraphQlViaEngine(
                CREATE_GROUP_CHAT_QUERY,
                mapOf("chat" to chat),
                user1Id
            ).errors!![0].message
            assertEquals(InvalidAdminIdException.message, response)
        }
    }

    @Nested
    inner class CreateTextMessage {
        @Test
        fun `The user must be able to create a message in a private chat they just deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            createTextMessage(user1Id, chatId, MessageText("t"))
        }

        @Test
        fun `Messaging in a chat the user isn't in must throw an exception`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            GroupChats.create(listOf(admin2Id))
            assertEquals(InvalidChatIdException.message, errCreateTextMessage(admin2Id, chatId, MessageText("t")))
        }

        @Test
        fun `The message must be created sans context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            createTextMessage(adminId, chatId, MessageText("t"))
            val contexts = Messages.readGroupChat(chatId, userId = adminId).map { it.node.context }
            assertEquals(listOf(MessageContext(hasContext = false, id = null)), contexts)
        }

        @Test
        fun `The message must be created with a context`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            createTextMessage(adminId, chatId, MessageText("t"), contextMessageId = messageId)
            val context = Messages.readGroupChat(chatId, userId = adminId)[1].node.context
            assertEquals(MessageContext(hasContext = true, id = messageId), context)
        }

        @Test
        fun `Using a nonexistent message context must fail`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val response = errCreateTextMessage(adminId, chatId, MessageText("t"), contextMessageId = 1)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `A non-admin mustn't be allowed to message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = executeGraphQlViaHttp(
                CREATE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "text" to "Hi"),
                user.accessToken
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class CreatePrivateChat {
        @Test
        fun `A chat must be created`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = createPrivateChat(user1Id, user2Id)
            assertEquals(listOf(chatId), PrivateChats.readIdList(user1Id))
        }

        @Test
        fun `Recreating a chat the user deleted must cause the existing chat's ID to be returned`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = createPrivateChat(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(chatId, createPrivateChat(user1Id, user2Id))
        }

        @Test
        fun `A chat mustn't be created with a nonexistent user`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidUserIdException.message, errCreatePrivateChat(userId, otherUserId = -1))
        }

        @Test
        fun `A chat mustn't be created with the user themselves`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidUserIdException.message, errCreatePrivateChat(userId, userId))
        }
    }

    /** A private chat between two users where [user2Id] sent the [messageId]. */
    data class UtilizedPrivateChat(val messageId: Int, val user1Id: Int, val user2Id: Int)

    @Nested
    inner class CreateStatus {
        private fun createUtilizedPrivateChat(): UtilizedPrivateChat {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            return UtilizedPrivateChat(messageId, user1Id, user2Id)
        }

        @Test
        fun `A status must be created`() {
            val (messageId, user1Id) = createUtilizedPrivateChat()
            createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            val statuses = MessageStatuses.read(messageId)
            assertEquals(1, statuses.size)
            assertEquals(MessageStatus.DELIVERED, statuses[0].status)
        }

        @Test
        fun `Creating a duplicate status must fail`() {
            val (messageId, user1Id) = createUtilizedPrivateChat()
            createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            assertEquals(DuplicateStatusException.message, errCreateStatus(user1Id, messageId, MessageStatus.DELIVERED))
        }

        @Test
        fun `Creating a status on the user's own message must fail`() {
            val (messageId, _, user2Id) = createUtilizedPrivateChat()
            val response = errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Creating a status on a message from a chat the user isn't in must fail`() {
            val (messageId) = createUtilizedPrivateChat()
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidMessageIdException.message, errCreateStatus(userId, messageId, MessageStatus.DELIVERED))
        }

        @Test
        fun `Creating a status on a nonexistent message must fail`() {
            val userId = createVerifiedUsers(1)[0].info.id
            val response = errCreateStatus(userId, messageId = 1, status = MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Creating a status in a private chat the user deleted must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val response = errCreateStatus(user1Id, messageId, MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }

        @Test
        fun `Creating a status on a message which was sent before the user deleted the private chat must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            val response = errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED)
            assertEquals(InvalidMessageIdException.message, response)
        }
    }

    @Nested
    inner class DeleteAccount {
        @Test
        fun `An account must be deleted from the auth system`() {
            val userId = createVerifiedUsers(1)[0].info.id
            deleteAccount(userId)
            assertFalse(Users.exists(userId))
        }

        @Test
        fun `An account mustn't be deleted if the user is the last admin of a group chat with other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertEquals(CannotDeleteAccountException.message, errDeleteAccount(adminId))
        }
    }

    @Nested
    inner class DeleteContacts {
        @Test
        fun `Contacts must be deleted, ignoring invalid ones`() {
            val (ownerId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            Contacts.create(ownerId, userIdList.toSet())
            deleteContacts(ownerId, userIdList + -1)
            assertTrue(Contacts.readIdList(ownerId).isEmpty())
        }
    }

    @Nested
    inner class DeleteMessage {
        @Test
        fun `The user's message must be deleted`() {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            deleteMessage(adminId, messageId)
            assertTrue(Messages.readGroupChat(chatId, userId = adminId).isEmpty())
        }

        @Test
        fun `Deleting a nonexistent message must return an error`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(userId, messageId = 0))
        }

        @Test
        fun `Deleting a message from a chat the user isn't in must throw an exception`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(userId, messageId))
        }

        @Test
        fun `Deleting another user's message must return an error`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(user1Id, messageId))
        }

        @Test
        fun `Deleting a message sent before the private chat was deleted by the user must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals(InvalidMessageIdException.message, errDeleteMessage(user1Id, messageId))
        }
    }

    @Nested
    inner class DeletePrivateChat {
        @Test
        fun `A chat must be deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            deletePrivateChat(user1Id, chatId)
            assertTrue(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `Deleting an invalid chat ID must throw an exception`() {
            val userId = createVerifiedUsers(1)[0].info.id
            assertEquals(InvalidChatIdException.message, errDeletePrivateChat(userId, chatId = 1))
        }
    }

    @Nested
    inner class EmailPasswordResetCode {
        @Test
        fun `A password reset request must be sent`() {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            emailPasswordResetCode(address)
        }

        @Test
        fun `Requesting a password reset for an unregistered address must throw an exception`() {
            assertEquals(UnregisteredEmailAddressException.message, errEmailPasswordResetCode("username@example.com"))
        }
    }

    @Nested
    inner class EmailEmailAddressVerification {
        @Test
        fun `A verification email must be sent`() {
            val address = "username@example.com"
            val account = AccountInput(Username("u"), Password("p"), address)
            Users.create(account)
            emailEmailAddressVerification(address)
        }

        @Test
        fun `Sending a verification email to an unregistered address must throw an exception`() {
            assertEquals(
                UnregisteredEmailAddressException.message,
                errEmailEmailAddressVerification("username@example.com")
            )
        }

        @Test
        fun `Sending a verification email to a verified address must fail`() {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            assertEquals(EmailAddressVerifiedException.message, errEmailEmailAddressVerification(address))
        }
    }

    @Nested
    inner class UpdateAccount {
        private fun testAccount(accountBeforeUpdate: Account, accountAfterUpdate: AccountUpdate) {
            assertFalse(isUsernameTaken(accountBeforeUpdate.username))
            with(Users.read(accountAfterUpdate.username!!)) {
                assertEquals(accountAfterUpdate.username, username)
                assertEquals(accountAfterUpdate.emailAddress, emailAddress)
                assertFalse(Users.read(id).hasVerifiedEmailAddress)
                assertEquals(accountBeforeUpdate.firstName, firstName)
                assertEquals(accountAfterUpdate.lastName, lastName)
                assertEquals(accountBeforeUpdate.bio, bio)
            }
        }

        @Test
        fun `Only the specified fields must be updated`() {
            val user = createVerifiedUsers(1)[0].info
            val update =
                AccountUpdate(Username("john_roger"), emailAddress = "john.roger@example.com", lastName = Name("Roger"))
            updateAccount(user.id, update)
            testAccount(user, update)
        }

        @Test
        fun `Updating a username to one already taken mustn't allow the account to be updated`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val response = errUpdateAccount(user1.id, AccountUpdate(username = user2.username))
            assertEquals(UsernameTakenException.message, response)
        }

        @Test
        fun `Updating an email to one already taken mustn't allow the account to be updated`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val response = errUpdateAccount(user1.id, AccountUpdate(emailAddress = user2.emailAddress))
            assertEquals(EmailAddressTakenException.message, response)
        }
    }
}
