package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import com.neelkamath.omniChat.testingObjectMapper
import java.util.*

const val LEAVE_GROUP_CHAT_QUERY = """
    mutation LeaveGroupChat(${"$"}chatId: Int!) {
        leaveGroupChat(chatId: ${"$"}chatId) {
            $LEAVE_GROUP_CHAT_RESULT_FRAGMENT
        }
    }
"""

fun leaveGroupChat(userId: Int, chatId: Int): LeaveGroupChatResult? {
    val data =
        executeGraphQlViaEngine(LEAVE_GROUP_CHAT_QUERY, mapOf("chatId" to chatId), userId).data!!["leaveGroupChat"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val JOIN_PUBLIC_CHAT_QUERY = """
    mutation JoinPublicChat(${"$"}chatId: Int!) {
        joinPublicChat(chatId: ${"$"}chatId) {
            $INVALID_CHAT_ID_FRAGMENT
        }
    }
"""

fun joinPublicChat(userId: Int, chatId: Int): InvalidChatId? {
    val data =
        executeGraphQlViaEngine(JOIN_PUBLIC_CHAT_QUERY, mapOf("chatId" to chatId), userId).data!!["joinPublicChat"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val UNBLOCK_USER_QUERY = """
    mutation UnblockUser(${"$"}id: Int!) {
        unblockUser(id: ${"$"}id)
    }
"""

fun unblockUser(userId: Int, blockedUserId: Int): Boolean {
    val data = executeGraphQlViaEngine(UNBLOCK_USER_QUERY, mapOf("id" to blockedUserId), userId)
        .data!!["unblockUser"]!!
    return testingObjectMapper.convertValue(data)
}

const val BLOCK_USER_QUERY = """
    mutation BlockUser(${"$"}id: Int!) {
        blockUser(id: ${"$"}id) {
            $INVALID_USER_ID_FRAGMENT
        }
    }
"""

fun blockUser(userId: Int, blockedUserId: Int): InvalidUserId? {
    val data = executeGraphQlViaEngine(BLOCK_USER_QUERY, mapOf("id" to blockedUserId), userId)
        .data!!["blockUser"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val RESET_PASSWORD_QUERY = """
    mutation ResetPassword(${"$"}emailAddress: String!, ${"$"}passwordResetCode: Int!, ${"$"}newPassword: Password!) {
        resetPassword(
            emailAddress: ${"$"}emailAddress
            passwordResetCode: ${"$"}passwordResetCode
            newPassword: ${"$"}newPassword
        ) {
            $RESET_PASSWORD_RESULT_FRAGMENT
        }
    }
"""

fun resetPassword(emailAddress: String, passwordResetCode: Int, newPassword: Password): ResetPasswordResult? {
    val data = executeGraphQlViaEngine(
        RESET_PASSWORD_QUERY,
        mapOf(
            "emailAddress" to emailAddress,
            "passwordResetCode" to passwordResetCode,
            "newPassword" to newPassword.value,
        ),
    ).data!!["resetPassword"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val VERIFY_EMAIL_ADDRESS_QUERY = """
    mutation VerifyEmailAddress(${"$"}emailAddress: String!, ${"$"}verificationCode: Int!) {
        verifyEmailAddress(emailAddress: ${"$"}emailAddress, verificationCode: ${"$"}verificationCode) {
            $VERIFY_EMAIL_ADDRESS_RESULT_FRAGMENT
        }
    }
"""

fun verifyEmailAddress(emailAddress: String, verificationCode: Int): VerifyEmailAddressResult? {
    val data = executeGraphQlViaEngine(
        VERIFY_EMAIL_ADDRESS_QUERY,
        mapOf("emailAddress" to emailAddress, "verificationCode" to verificationCode),
    ).data!!["verifyEmailAddress"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val TRIGGER_ACTION_QUERY = """
    mutation TriggerAction(${"$"}messageId: Int!, ${"$"}action: MessageText!) {
        triggerAction(messageId: ${"$"}messageId, action: ${"$"}action) {
            $TRIGGER_ACTION_RESULT_FRAGMENT
        }
    }
"""

fun triggerAction(userId: Int, messageId: Int, action: MessageText): TriggerActionResult? {
    val data = executeGraphQlViaEngine(
        TRIGGER_ACTION_QUERY,
        mapOf("messageId" to messageId, "action" to action),
        userId,
    ).data!!["triggerAction"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val CREATE_ACTION_MESSAGE_QUERY = """
    mutation CreateActionMessage(${"$"}chatId: Int!, ${"$"}message: ActionMessageInput!, ${"$"}contextMessageId: Int) {
        createActionMessage(chatId: ${"$"}chatId, message: ${"$"}message, contextMessageId: ${"$"}contextMessageId) {
            $CREATE_ACTION_MESSAGE_RESULT_FRAGMENT
        }
    }
"""

fun createActionMessage(
    userId: Int,
    chatId: Int,
    message: ActionMessageInput,
    contextMessageId: Int? = null,
): CreateActionMessageResult? {
    val data = executeGraphQlViaEngine(
        CREATE_ACTION_MESSAGE_QUERY,
        mapOf("chatId" to chatId, "message" to message, "contextMessageId" to contextMessageId),
        userId,
    ).data!!["createActionMessage"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val FORWARD_MESSAGE_QUERY = """
    mutation ForwardMessage(${"$"}chatId: Int!, ${"$"}messageId: Int!, ${"$"}contextMessageId: Int) {
        forwardMessage(chatId: ${"$"}chatId, messageId: ${"$"}messageId, contextMessageId: ${"$"}contextMessageId) {
            $FORWARD_MESSAGE_RESULT_FRAGMENT
        }
    }
"""

fun forwardMessage(userId: Int, chatId: Int, messageId: Int, contextMessageId: Int? = null): ForwardMessageResult? {
    val data = executeGraphQlViaEngine(
        FORWARD_MESSAGE_QUERY,
        mapOf("chatId" to chatId, "messageId" to messageId, "contextMessageId" to contextMessageId),
        userId,
    ).data!!["forwardMessage"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val REMOVE_GROUP_CHAT_USERS_QUERY = """
    mutation RemoveGroupChatUsers(${"$"}chatId: Int!, ${"$"}idList: [Int!]!) {
        removeGroupChatUsers(chatId: ${"$"}chatId, idList: ${"$"}idList) {
            $CANNOT_LEAVE_CHAT_FRAGMENT
        }
    }
"""

fun removeGroupChatUsers(userId: Int, chatId: Int, idList: List<Int>): CannotLeaveChat? {
    val data = executeGraphQlViaEngine(
        REMOVE_GROUP_CHAT_USERS_QUERY,
        mapOf("chatId" to chatId, "idList" to idList),
        userId,
    ).data!!["removeGroupChatUsers"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val SET_INVITABILITY_QUERY = """
    mutation SetInvitability(${"$"}chatId: Int!, ${"$"}isInvitable: Boolean!) {
        setInvitability(chatId: ${"$"}chatId, isInvitable: ${"$"}isInvitable) {
            $INVALID_CHAT_ID_FRAGMENT
        }
    }
"""

fun setInvitability(userId: Int, chatId: Int, isInvitable: Boolean): InvalidChatId? {
    val data = executeGraphQlViaEngine(
        SET_INVITABILITY_QUERY,
        mapOf("chatId" to chatId, "isInvitable" to isInvitable),
        userId,
    ).data!!["setInvitability"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY = """
    mutation CreateGroupChatInviteMessage(${"$"}chatId: Int!, ${"$"}invitedChatId: Int!, ${"$"}contextMessageId: Int) {
        createGroupChatInviteMessage(
            chatId: ${"$"}chatId
            invitedChatId: ${"$"}invitedChatId
            contextMessageId: ${"$"}contextMessageId
        ) {
            $CREATE_GROUP_CHAT_INVITE_MESSAGE_RESULT_FRAGMENT
        }
    }
"""

fun createGroupChatInviteMessage(
    userId: Int,
    chatId: Int,
    invitedChatId: Int,
    contextMessageId: Int? = null,
): CreateGroupChatInviteMessageResult? {
    val data = executeGraphQlViaEngine(
        CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
        mapOf("chatId" to chatId, "invitedChatId" to invitedChatId, "contextMessageId" to contextMessageId),
        userId,
    ).data!!["createGroupChatInviteMessage"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val JOIN_GROUP_CHAT_QUERY = """
    mutation JoinGroupChat(${"$"}inviteCode: Uuid!) {
        joinGroupChat(inviteCode: ${"$"}inviteCode) {
            $INVALID_INVITE_CODE_FRAGMENT
        }
    }
"""

fun joinGroupChat(userId: Int, inviteCode: UUID): InvalidInviteCode? {
    val data = executeGraphQlViaEngine(JOIN_GROUP_CHAT_QUERY, mapOf("inviteCode" to inviteCode), userId)
        .data!!["joinGroupChat"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val CREATE_POLL_MESSAGE_QUERY = """
    mutation CreatePollMessage(${"$"}chatId: Int!, ${"$"}poll: PollInput!, ${"$"}contextMessageId: Int) {
        createPollMessage(chatId: ${"$"}chatId, poll: ${"$"}poll, contextMessageId: ${"$"}contextMessageId) {
            $CREATE_POLL_MESSAGE_RESULT_FRAGMENT
        }
    }
"""

fun createPollMessage(
    userId: Int,
    chatId: Int,
    poll: PollInput,
    contextMessageId: Int? = null,
): CreatePollMessageResult? {
    val data = executeGraphQlViaEngine(
        CREATE_POLL_MESSAGE_QUERY,
        mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to contextMessageId),
        userId,
    ).data!!["createPollMessage"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val SET_POLL_VOTE_QUERY = """
    mutation SetPollVote(${"$"}messageId: Int!, ${"$"}option: MessageText!, ${"$"}vote: Boolean!) {
        setPollVote(messageId: ${"$"}messageId, option: ${"$"}option, vote: ${"$"}vote) {
            $SET_POLL_VOTE_RESULT_FRAGMENT
        }
    }
"""

fun setPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): SetPollVoteResult? {
    val data = executeGraphQlViaEngine(
        SET_POLL_VOTE_QUERY,
        mapOf("messageId" to messageId, "option" to option, "vote" to vote),
        userId,
    ).data!!["setPollVote"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val SET_BROADCAST_QUERY = """
    mutation SetBroadcast(${"$"}chatId: Int!, ${"$"}isBroadcast: Boolean!) {
        setBroadcast(chatId: ${"$"}chatId, isBroadcast: ${"$"}isBroadcast)
    }
"""

fun setBroadcast(userId: Int, chatId: Int, isBroadcast: Boolean): Placeholder {
    val data = executeGraphQlViaEngine(
        SET_BROADCAST_QUERY,
        mapOf("chatId" to chatId, "isBroadcast" to isBroadcast),
        userId,
    ).data!!["setBroadcast"]!!
    return testingObjectMapper.convertValue(data)
}

const val MAKE_GROUP_CHAT_ADMINS_QUERY = """
    mutation MakeGroupChatAdmins(${"$"}chatId: Int!, ${"$"}idList: [Int!]!) {
        makeGroupChatAdmins(chatId: ${"$"}chatId, idList: ${"$"}idList)
    }
"""

fun makeGroupChatAdmins(userId: Int, chatId: Int, idList: List<Int>): Placeholder {
    val data = executeGraphQlViaEngine(
        MAKE_GROUP_CHAT_ADMINS_QUERY,
        mapOf("chatId" to chatId, "idList" to idList),
        userId,
    ).data!!["makeGroupChatAdmins"]!!
    return testingObjectMapper.convertValue(data)
}

const val ADD_GROUP_CHAT_USERS_QUERY = """
    mutation AddGroupChatUsers(${"$"}chatId: Int!, ${"$"}idList: [Int!]!) {
        addGroupChatUsers(chatId: ${"$"}chatId, idList: ${"$"}idList)
    }
"""

fun addGroupChatUsers(userId: Int, chatId: Int, idList: List<Int>): Placeholder {
    val data = executeGraphQlViaEngine(
        ADD_GROUP_CHAT_USERS_QUERY,
        mapOf("chatId" to chatId, "idList" to idList),
        userId,
    ).data!!["addGroupChatUsers"]!!
    return testingObjectMapper.convertValue(data)
}

const val UPDATE_GROUP_CHAT_DESCRIPTION_QUERY = """
    mutation UpdateGroupChatDescription(${"$"}chatId: Int!, ${"$"}description: GroupChatDescription!) {
        updateGroupChatDescription(chatId: ${"$"}chatId, description: ${"$"}description)
    }
"""

fun updateGroupChatDescription(userId: Int, chatId: Int, description: GroupChatDescription): Placeholder {
    val data = executeGraphQlViaEngine(
        UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
        mapOf("chatId" to chatId, "description" to description.value),
        userId,
    ).data!!["updateGroupChatDescription"]!!
    return testingObjectMapper.convertValue(data)
}

const val UPDATE_GROUP_CHAT_TITLE_QUERY = """
    mutation UpdateGroupChatTitle(${"$"}chatId: Int!, ${"$"}title: GroupChatTitle!) {
        updateGroupChatTitle(chatId: ${"$"}chatId, title: ${"$"}title)
    }
"""

fun updateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): Placeholder {
    val data = executeGraphQlViaEngine(
        UPDATE_GROUP_CHAT_TITLE_QUERY,
        mapOf("chatId" to chatId, "title" to title.value),
        userId,
    ).data!!["updateGroupChatTitle"]!!
    return testingObjectMapper.convertValue(data)
}

const val UNSTAR_QUERY = """
    mutation Unstar(${"$"}messageId: Int!) {
        unstar(messageId: ${"$"}messageId)
    }
"""

fun unstar(userId: Int, messageId: Int): Placeholder {
    val data = executeGraphQlViaEngine(UNSTAR_QUERY, mapOf("messageId" to messageId), userId).data!!["unstar"]!!
    return testingObjectMapper.convertValue(data)
}

const val STAR_QUERY = """
    mutation Star(${"$"}messageId: Int!) {
        star(messageId: ${"$"}messageId) {
            $INVALID_MESSAGE_ID_FRAGMENT
        }
    }
"""

fun star(userId: Int, messageId: Int): InvalidMessageId? {
    val data = executeGraphQlViaEngine(STAR_QUERY, mapOf("messageId" to messageId), userId).data!!["star"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val SET_ONLINE_QUERY = """
    mutation SetOnline(${"$"}isOnline: Boolean!) {
        setOnline(isOnline: ${"$"}isOnline)
    }
"""

fun setOnline(userId: Int, isOnline: Boolean): Placeholder {
    val data = executeGraphQlViaEngine(SET_ONLINE_QUERY, mapOf("isOnline" to isOnline), userId)
        .data!!["setOnline"]!!
    return testingObjectMapper.convertValue(data)
}

const val SET_TYPING_QUERY = """
    mutation SetTyping(${"$"}chatId: Int!, ${"$"}isTyping: Boolean!) {
        setTyping(chatId: ${"$"}chatId, isTyping: ${"$"}isTyping) {
            $INVALID_CHAT_ID_FRAGMENT
        }
    }
"""

fun setTyping(userId: Int, chatId: Int, isTyping: Boolean): InvalidChatId? {
    val data = executeGraphQlViaEngine(SET_TYPING_QUERY, mapOf("chatId" to chatId, "isTyping" to isTyping), userId)
        .data!!["setTyping"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val DELETE_GROUP_CHAT_PIC_QUERY = """
    mutation DeleteGroupChatPic(${"$"}chatId: Int!) {
        deleteGroupChatPic(chatId: ${"$"}chatId)
    }
"""

fun deleteGroupChatPic(userId: Int, chatId: Int): Placeholder {
    val data = executeGraphQlViaEngine(DELETE_GROUP_CHAT_PIC_QUERY, mapOf("chatId" to chatId), userId)
        .data!!["deleteGroupChatPic"]!!
    return testingObjectMapper.convertValue(data)
}

const val DELETE_PROFILE_PIC_QUERY = """
    mutation DeleteProfilePic {
        deleteProfilePic
    }
"""

fun deleteProfilePic(userId: Int): Placeholder {
    val data = executeGraphQlViaEngine(DELETE_PROFILE_PIC_QUERY, userId = userId).data!!["deleteProfilePic"]!!
    return testingObjectMapper.convertValue(data)
}

const val CREATE_ACCOUNTS_QUERY = """
    mutation CreateAccount(${"$"}account: AccountInput!) {
        createAccount(account: ${"$"}account) {
            $CREATE_ACCOUNT_RESULT_FRAGMENT
        }
    }
"""

fun createAccount(account: AccountInput): CreateAccountResult? {
    val data = executeGraphQlViaEngine(CREATE_ACCOUNTS_QUERY, mapOf("account" to account))
        .data!!["createAccount"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val CREATE_CONTACT_QUERY = """
    mutation CreateContact(${"$"}id: Int!) {
        createContact(id: ${"$"}id)
    }
"""

fun createContact(userId: Int, id: Int): Boolean {
    val data = executeGraphQlViaEngine(CREATE_CONTACT_QUERY, mapOf("id" to id), userId).data!!["createContact"]!!
    return testingObjectMapper.convertValue(data)
}

const val CREATE_GROUP_CHAT_QUERY = """
    mutation CreateGroupChat(${"$"}chat: GroupChatInput!) {
        createGroupChat(chat: ${"$"}chat) {
            $CREATE_GROUP_CHAT_RESULT_FRAGMENT
        }
    }
"""

const val CREATE_MESSAGE_QUERY = """
    mutation CreateTextMessage(${"$"}chatId: Int!, ${"$"}text: MessageText!, ${"$"}contextMessageId: Int) {
        createTextMessage(chatId: ${"$"}chatId, text: ${"$"}text, contextMessageId: ${"$"}contextMessageId) {
            $CREATE_TEXT_MESSAGE_RESULT_FRAGMENT
        }
    }
"""

fun createTextMessage(
    userId: Int,
    chatId: Int,
    text: MessageText,
    contextMessageId: Int? = null,
): CreateTextMessageResult? {
    val data = executeGraphQlViaEngine(
        CREATE_MESSAGE_QUERY,
        mapOf("chatId" to chatId, "text" to text, "contextMessageId" to contextMessageId),
        userId,
    ).data!!["createTextMessage"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val CREATE_PRIVATE_CHAT_QUERY = """
    mutation CreatePrivateChat(${"$"}userId: Int!) {
        createPrivateChat(userId: ${"$"}userId) {
            $CREATE_PRIVATE_CHAT_RESULT_FRAGMENT
        }
    }
"""

fun createPrivateChat(userId: Int, otherUserId: Int): CreatePrivateChatResult {
    val data = executeGraphQlViaEngine(CREATE_PRIVATE_CHAT_QUERY, mapOf("userId" to otherUserId), userId)
        .data!!["createPrivateChat"]!!
    return testingObjectMapper.convertValue(data)
}

const val CREATE_STATUS_QUERY = """
    mutation CreateStatus(${"$"}messageId: Int!, ${"$"}status: MessageStatus!) {
        createStatus(messageId: ${"$"}messageId, status: ${"$"}status) {
            $INVALID_MESSAGE_ID_FRAGMENT
        }
    }
"""

fun createStatus(userId: Int, messageId: Int, status: MessageStatus): InvalidMessageId? {
    val data = executeGraphQlViaEngine(CREATE_STATUS_QUERY, mapOf("messageId" to messageId, "status" to status), userId)
        .data!!["createStatus"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val DELETE_ACCOUNT_QUERY = """
    mutation DeleteAccount {
        deleteAccount {
            $CANNOT_DELETE_ACCOUNT_FRAGMENT
        }
    }
"""

fun deleteAccount(userId: Int): CannotDeleteAccount? {
    val data = executeGraphQlViaEngine(DELETE_ACCOUNT_QUERY, userId = userId).data!!["deleteAccount"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val DELETE_CONTACT_QUERY = """
    mutation DeleteContact(${"$"}id: Int!) {
        deleteContact(id: ${"$"}id)
    }
"""

fun deleteContact(userId: Int, id: Int): Boolean {
    val data = executeGraphQlViaEngine(DELETE_CONTACT_QUERY, mapOf("id" to id), userId).data!!["deleteContact"]!!
    return testingObjectMapper.convertValue(data)
}

const val DELETE_MESSAGE_QUERY = """
    mutation DeleteMessage(${"$"}id: Int!) {
        deleteMessage(id: ${"$"}id) {
            $INVALID_MESSAGE_ID_FRAGMENT
        }
    }
"""

fun deleteMessage(userId: Int, messageId: Int): InvalidMessageId? {
    val data = executeGraphQlViaEngine(DELETE_MESSAGE_QUERY, mapOf("id" to messageId), userId)
        .data!!["deleteMessage"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val DELETE_PRIVATE_CHAT_QUERY = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId) {
            $INVALID_CHAT_ID_FRAGMENT
        }
    }
"""

fun deletePrivateChat(userId: Int, chatId: Int): InvalidChatId? {
    val data = executeGraphQlViaEngine(DELETE_PRIVATE_CHAT_QUERY, mapOf("chatId" to chatId), userId)
        .data!!["deletePrivateChat"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val EMAIL_PASSWORD_RESET_CODE_QUERY = """
    mutation EmailPasswordResetCode(${"$"}emailAddress: String!) {
        emailPasswordResetCode(emailAddress: ${"$"}emailAddress) {
            $UNREGISTERED_EMAIL_ADDRESS_FRAGMENT
        }
    }
"""

fun emailPasswordResetCode(emailAddress: String): UnregisteredEmailAddress? {
    val data = executeGraphQlViaEngine(EMAIL_PASSWORD_RESET_CODE_QUERY, mapOf("emailAddress" to emailAddress))
        .data!!["emailPasswordResetCode"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val EMAIL_EMAIL_ADDRESS_VERIFICATION_QUERY = """
    mutation EmailEmailAddressVerification(${"$"}emailAddress: String!) {
        emailEmailAddressVerification(emailAddress: ${"$"}emailAddress) {
            $EMAIL_EMAIL_ADDRESS_VERIFICATION_RESULT_FRAGMENT
        }
    }
"""

fun emailEmailAddressVerification(emailAddress: String): EmailEmailAddressVerificationResult? {
    val data = executeGraphQlViaEngine(EMAIL_EMAIL_ADDRESS_VERIFICATION_QUERY, mapOf("emailAddress" to emailAddress))
        .data!!["emailEmailAddressVerification"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}

const val UPDATE_ACCOUNT_QUERY = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update) {
            $UPDATE_ACCOUNT_RESULT_FRAGMENT
        }
    }
"""

fun updateAccount(userId: Int, update: AccountUpdate): UpdateAccountResult? {
    val data = executeGraphQlViaEngine(UPDATE_ACCOUNT_QUERY, mapOf("update" to update), userId)
        .data!!["updateAccount"]
    return if (data == null) data else testingObjectMapper.convertValue(data)
}
