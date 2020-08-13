package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.Pic
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.util.*

const val FORWARD_MESSAGE_QUERY = """
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

const val REMOVE_GROUP_CHAT_USERS_QUERY = """
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

const val SET_INVITABILITY_QUERY = """
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

const val CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY = """
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

const val JOIN_GROUP_CHAT_QUERY = """
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

const val CREATE_POLL_MESSAGE_QUERY = """
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

const val SET_POLL_VOTE_QUERY = """
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

const val SET_BROADCAST_STATUS_QUERY = """
    mutation SetBroadcastStatus(${"$"}chatId: Int!, ${"$"}isBroadcast: Boolean!) {
        setBroadcastStatus(chatId: ${"$"}chatId, isBroadcast: ${"$"}isBroadcast)
    }
"""

private fun operateSetBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_BROADCAST_STATUS_QUERY, mapOf("chatId" to chatId, "isBroadcast" to isBroadcast), userId)

fun setBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): Placeholder {
    val data = operateSetBroadcastStatus(userId, chatId, isBroadcast).data!!["setBroadcastStatus"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSetBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): String =
    operateSetBroadcastStatus(userId, chatId, isBroadcast).errors!![0].message

const val MAKE_GROUP_CHAT_ADMINS_QUERY = """
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

const val ADD_GROUP_CHAT_USERS_QUERY = """
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

const val UPDATE_GROUP_CHAT_DESCRIPTION_QUERY = """
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

const val DELETE_STAR_QUERY = """
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

const val STAR_QUERY = """
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

const val SET_ONLINE_STATUS_QUERY = """
    mutation SetOnlineStatus(${"$"}isOnline: Boolean!) {
        setOnlineStatus(isOnline: ${"$"}isOnline)
    }
"""

private fun operateSetOnlineStatus(userId: Int, isOnline: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_ONLINE_STATUS_QUERY, mapOf("isOnline" to isOnline), userId)

fun setOnlineStatus(userId: Int, isOnline: Boolean): Placeholder {
    val data = operateSetOnlineStatus(userId, isOnline).data!!["setOnlineStatus"] as String
    return testingObjectMapper.convertValue(data)
}

const val SET_TYPING_QUERY = """
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

const val DELETE_GROUP_CHAT_PIC_QUERY = """
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

const val DELETE_PROFILE_PIC_QUERY = """
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

const val CREATE_ACCOUNTS_QUERY = """
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

const val CREATE_CONTACTS_QUERY = """
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

const val CREATE_GROUP_CHAT_QUERY = """
    mutation CreateGroupChat(${"$"}chat: GroupChatInput!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

private fun operateCreateGroupChat(userId: Int, chat: GroupChatInput): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), userId)

fun errCreateGroupChat(userId: Int, chat: GroupChatInput): String =
    operateCreateGroupChat(userId, chat).errors!![0].message

const val CREATE_MESSAGE_QUERY = """
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

const val CREATE_PRIVATE_CHAT_QUERY = """
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

const val CREATE_STATUS_QUERY = """
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

const val DELETE_ACCOUNT_QUERY = """
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

const val DELETE_CONTACTS_QUERY = """
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

const val DELETE_MESSAGE_QUERY = """
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

const val DELETE_PRIVATE_CHAT_QUERY = """
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

const val RESET_PASSWORD_QUERY = """
    mutation ResetPassword(${"$"}emailAddress: String!) {
        resetPassword(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateResetPassword(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(RESET_PASSWORD_QUERY, mapOf("emailAddress" to emailAddress))

fun resetPassword(emailAddress: String): Placeholder {
    val data = operateResetPassword(emailAddress).data!!["resetPassword"] as String
    return testingObjectMapper.convertValue(data)
}

fun errResetPassword(emailAddress: String): String = operateResetPassword(emailAddress).errors!![0].message

const val SEND_EMAIL_ADDRESS_VERIFICATION_QUERY = """
    mutation SendEmailAddressVerification(${"$"}emailAddress: String!) {
        sendEmailAddressVerification(emailAddress: ${"$"}emailAddress)
    }
"""

private fun operateSendEmailAddressVerification(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(SEND_EMAIL_ADDRESS_VERIFICATION_QUERY, mapOf("emailAddress" to emailAddress))

fun sendEmailAddressVerification(emailAddress: String): Placeholder {
    val data = operateSendEmailAddressVerification(emailAddress).data!!["sendEmailAddressVerification"] as String
    return testingObjectMapper.convertValue(data)
}

fun errSendEmailVerification(emailAddress: String): String =
    operateSendEmailAddressVerification(emailAddress).errors!![0].message

const val UPDATE_ACCOUNT_QUERY = """
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

class MutationsTest : FunSpec({
    context("forwardMessage(DataFetchingEnvironment)") {
        test("The message should be forwarded with a context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chat1Id, chat2Id) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val messageId = Messages.message(adminId, chat1Id)
            val contextMessageId = Messages.message(adminId, chat2Id)
            forwardMessage(adminId, chat2Id, messageId, contextMessageId)
            with(Messages.readGroupChat(chat2Id).last().node) {
                context.id shouldBe contextMessageId
                isForwarded.shouldBeTrue()
            }
        }

        test("A non-admin shouldn't be allowed to forward a message to a broadcast chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val messageId = Messages.message(admin.info.id, chatId)
            executeGraphQlViaHttp(
                FORWARD_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "messageId" to messageId),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }

        test("Messaging in a chat the user isn't in should fail") {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            errForwardMessage(admin1Id, chat2Id, messageId) shouldBe InvalidChatIdException.message
        }

        test("Forwarding a nonexistent message should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            errForwardMessage(adminId, chatId, messageId = 1) shouldBe InvalidMessageIdException.message
        }

        test("Using an invalid context message should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            errForwardMessage(adminId, chatId, messageId, contextMessageId = 1) shouldBe
                    InvalidMessageIdException.message
        }

        test("Forwarding a message the user can't see should fail") {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            errForwardMessage(admin2Id, chat2Id, messageId) shouldBe InvalidMessageIdException.message
        }
    }

    context("setInvitability(DataFetchingEnvironment") {
        test("The chat's invitability should be updated") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            setInvitability(adminId, chatId, isInvitable = true)
            GroupChats.readChat(chatId).isInvitable.shouldBeTrue()
        }

        test("Updating a public chat should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId), isPublic = true)
            errSetInvitability(adminId, chatId, isInvitable = true) shouldBe InvalidChatIdException.message
        }

        test("An error should be returned when a non-admin updates the invitability") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                SET_INVITABILITY_QUERY,
                mapOf("chatId" to chatId, "isInvitable" to true),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("createGroupChatInviteMessage(DataFetchingEnvironment)") {
        test("A message should be created with a context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2).map { GroupChats.create(listOf(adminId), isInvitable = true) }
            val contextMessageId = Messages.message(adminId, chatId, MessageText("t"))
            createGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId)
            GroupChatInviteMessages.count() shouldBe 1
        }

        test("Messaging in a broadcast chat should fail") {
            val (admin1, admin2) = createVerifiedUsers(2)
            val chatId = GroupChats.create(
                adminIdList = listOf(admin1.info.id),
                userIdList = listOf(admin2.info.id),
                isBroadcast = true
            )
            val invitedChatId = GroupChats.create(listOf(admin2.info.id), isInvitable = true)
            executeGraphQlViaHttp(
                CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "invitedChatId" to invitedChatId),
                admin2.accessToken
            ).shouldHaveUnauthorizedStatus()
        }

        test("Creating a message in a chat the user isn't in should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val invitedChatId = GroupChats.create(listOf(adminId))
            errCreateGroupChatInviteMessage(adminId, chatId = 1, invitedChatId = invitedChatId) shouldBe
                    InvalidChatIdException.message
        }

        test("Inviting users to a private chat should fail") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminIdList = listOf(user1Id))
            val invitedChatId = PrivateChats.create(user1Id, user2Id)
            errCreateGroupChatInviteMessage(user1Id, chatId, invitedChatId) shouldBe InvalidInvitedChatException.message
        }

        test("Inviting users to a group chat with invites turned off should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            errCreateGroupChatInviteMessage(adminId, chatId, invitedChatId) shouldBe
                    InvalidInvitedChatException.message
        }

        test("Using an invalid content message should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val (chatId, invitedChatId) = listOf(1, 2).map { GroupChats.create(listOf(adminId), isInvitable = true) }
            errCreateGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId = 1) shouldBe
                    InvalidMessageIdException.message
        }
    }

    context("joinGroupChat(DataFetchingEnvironment)") {
        test("An invite code should be used to join the chat, even if the chat has already been joined") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            repeat(2) { joinGroupChat(userId, GroupChats.readInviteCode(chatId)) }
            GroupChatUsers.readUserIdList(chatId) shouldContainExactlyInAnyOrder listOf(adminId, userId)
        }

        test("Using an invalid invite code should fail") {
            val userId = createVerifiedUsers(1)[0].info.id
            errJoinGroupChat(userId, inviteCode = UUID.randomUUID()) shouldBe InvalidInviteCodeException.message
        }
    }

    context("createPollMessage(DataFetchingEnvironment)") {
        test("A poll message should be created with a context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            createPollMessage(adminId, chatId, poll, contextMessageId)
            val message = Messages.readGroupChat(chatId, userId = adminId).last().node
            message.context.id shouldBe contextMessageId
            val options = poll.options.map { PollOption(it, votes = listOf()) }
            PollMessages.read(message.messageId) shouldBe Poll(poll.title, options)
        }

        test("Messaging a poll in a chat the user isn't in should fail") {
            val userId = createVerifiedUsers(1)[0].info.id
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            errCreatePollMessage(userId, chatId = 1, poll = poll) shouldBe InvalidChatIdException.message
        }

        test("Creating a poll in response to a nonexistent message should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            errCreatePollMessage(adminId, chatId, poll, contextMessageId = 1) shouldBe InvalidMessageIdException.message
        }

        test("Using an invalid poll should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = mapOf("title" to "Title", "options" to listOf("option"))
            executeGraphQlViaEngine(
                CREATE_POLL_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to null),
                adminId
            ).errors!![0].message shouldBe InvalidPollException.message
        }
    }

    context("setPollVote(DataFetchingEnvironment)") {
        test("The user's vote should be updated") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val option = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(option, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            setPollVote(adminId, messageId, option, vote = true)
            PollMessages.read(messageId).options.first { it.option == option }.votes shouldBe listOf(adminId)
        }

        test("Voting on a nonexistent poll should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            errSetPollVote(adminId, messageId = 1, option = MessageText("option"), vote = true) shouldBe
                    InvalidMessageIdException.message
        }

        test("Voting for a nonexistent option should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            errSetPollVote(adminId, messageId, MessageText("nonexistent option"), vote = true) shouldBe
                    NonexistentOptionException.message
        }
    }

    context("setBroadcastStatus(DataFetchingEnvironment)") {
        test("Only an admin should be allowed to set the broadcast status") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                SET_BROADCAST_STATUS_QUERY,
                mapOf("chatId" to chatId, "isBroadcast" to true),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }

        test("The broadcast status should be updated") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val isBroadcast = true
            setBroadcastStatus(adminId, chatId, isBroadcast)
            GroupChats.readChat(chatId, userId = adminId).isBroadcast shouldBe isBroadcast
        }
    }

    context("makeGroupChatAdmins(DataFetchingEnvironment)") {
        test("The users should be made admins") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            makeGroupChatAdmins(adminId, chatId, listOf(userId))
            GroupChatUsers.readAdminIdList(chatId) shouldBe listOf(adminId, userId)
        }

        test("Making a user who isn't in the chat an admin should fail") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            errMakeGroupChatAdmins(adminId, chatId, listOf(userId)) shouldBe InvalidUserIdException.message
        }

        test("A non-admin shouldn't be allowed to make users admins") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                MAKE_GROUP_CHAT_ADMINS_QUERY,
                mapOf("chatId" to chatId, "userIdList" to listOf<Int>()),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("addGroupChatUsers(DataFetchingEnvironment)") {
        test("Users should be added to the chat while ignoring duplicates and existing users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            addGroupChatUsers(adminId, chatId, listOf(adminId, userId, userId))
            GroupChatUsers.readUserIdList(chatId) shouldBe listOf(adminId, userId)
        }

        test("An exception should be thrown when adding a nonexistent user") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val invalidUserId = -1
            errAddGroupChatUsers(adminId, chatId, listOf(invalidUserId)) shouldBe InvalidUserIdException.message
        }

        test("A non-admin shouldn't be allowed to update the chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                ADD_GROUP_CHAT_USERS_QUERY,
                mapOf("chatId" to chatId, "userIdList" to listOf<Int>()),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("removeGroupChatUsers(DataFetchingEnvironment)") {
        test("The admin should be allowed to remove themselves along with non-admins if they aren't the last admin") {
            val (admin1Id, admin2Id, userId) = createVerifiedUsers(3).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id, admin2Id), listOf(userId))
            removeGroupChatUsers(admin1Id, chatId, listOf(admin1Id, userId))
            GroupChats.readChat(chatId).users.edges.map { it.node.id } shouldBe listOf(admin2Id)
        }

        test("Removing the last admin should be allowed if there won't be any remaining users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            removeGroupChatUsers(adminId, chatId, listOf(adminId, userId))
            GroupChats.count().shouldBeZero()
        }

        test("Removing the last admin shouldn't be allowed if there are other users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            errRemoveGroupChatUsers(adminId, chatId, listOf(adminId)) shouldBe InvalidUserIdException.message
        }

        test("Removing a nonexistent user should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            errRemoveGroupChatUsers(adminId, chatId, listOf(-1)) shouldBe InvalidUserIdException.message
        }
    }

    context("updateGroupChatDescription(DataFetchingEnvironment)") {
        test("The admin should update the description") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val description = GroupChatDescription("New description.")
            updateGroupChatDescription(adminId, chatId, description)
            GroupChats.readChat(chatId, userId = adminId).description shouldBe description
        }

        test("A non-admin shouldn't be allowed to update the chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
                mapOf("chatId" to chatId, "description" to "d"),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("updateGroupChatTitle(DataFetchingEnvironment)") {
        test("The admin should update the title") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val title = GroupChatTitle("New Title")
            updateGroupChatTitle(adminId, chatId, title)
            GroupChats.readChat(chatId, userId = adminId).title shouldBe title
        }

        test("A non-admin shouldn't be allowed to update the chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_TITLE_QUERY,
                mapOf("chatId" to chatId, "title" to "T"),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("deleteStar(DataFetchingEnvironment)") {
        test("A message should be starred") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            deleteStar(adminId, messageId)
            Stargazers.hasStar(adminId, messageId).shouldBeFalse()
        }
    }

    context("star(DataFetchingEnvironment)") {
        test("A message should be starred") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            star(adminId, messageId)
            Stargazers.read(adminId) shouldBe listOf(messageId)
        }

        test("Starring a message from a chat the user isn't in should fail") {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            val messageId = Messages.message(admin1Id, chatId)
            errStar(admin2Id, messageId) shouldBe InvalidMessageIdException.message
        }
    }

    context("setOnlineStatus(DataFetchingEnvironment)") {
        fun assertOnlineStatus(isOnline: Boolean) {
            val userId = createVerifiedUsers(1)[0].info.id
            setOnlineStatus(userId, isOnline)
            Users.read(userId).isOnline shouldBe isOnline
        }

        test("""The user's online status should be set to "true"""") { assertOnlineStatus(true) }

        test("""The user's online status should be set to "false"""") { assertOnlineStatus(false) }
    }

    context("setTyping(DataFetchingEnvironment)") {
        fun assertTypingStatus(isTyping: Boolean) {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            setTyping(adminId, chatId, isTyping)
            TypingStatuses.read(chatId, adminId) shouldBe isTyping
        }

        test("""The user's typing status should be set to "true"""") { assertTypingStatus(isTyping = true) }

        test("""The user's typing status should be set to "false"""") { assertTypingStatus(isTyping = false) }

        test("Setting the typing status in a chat the user isn't in should fail") {
            val userId = createVerifiedUsers(1)[0].info.id
            errSetTyping(userId, chatId = 1, isTyping = true) shouldBe InvalidChatIdException.message
        }
    }

    context("deleteGroupChatPic(DataFetchingEnvironment)") {
        test("Deleting the pic should remove it") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            GroupChats.updatePic(chatId, Pic(ByteArray(1), Pic.Type.PNG))
            deleteGroupChatPic(adminId, chatId)
            GroupChats.readPic(chatId).shouldBeNull()
        }

        test("An exception should be thrown when a non-admin updates the pic") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                DELETE_GROUP_CHAT_PIC_QUERY,
                mapOf("chatId" to chatId),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("deleteProfilePic(DataFetchingEnvironment)") {
        test("The user's profile pic should be deleted") {
            val userId = createVerifiedUsers(1)[0].info.id
            val pic = Pic(ByteArray(1), Pic.Type.PNG)
            Users.updatePic(userId, pic)
            deleteProfilePic(userId)
            Users.read(userId).pic.shouldBeNull()
        }
    }

    context("createAccount(DataFetchingEnvironment)") {
        test("Creating an account should save it to the auth system, and the DB") {
            val account = AccountInput(Username("username"), Password("password"), "username@example.com")
            createAccount(account)
            with(readUserByUsername(account.username)) {
                username shouldBe account.username
                emailAddress shouldBe account.emailAddress
            }
            Users.count() shouldBe 1
        }

        test("An account with a taken username shouldn't be created") {
            val account = AccountInput(Username("username"), Password("password"), "username@example.com")
            createAccount(account)
            errCreateAccount(account) shouldBe UsernameTakenException.message
        }

        test("An account with a taken email shouldn't be created") {
            val address = "username@example.com"
            val account = AccountInput(Username("username1"), Password("p"), address)
            createAccount(account)
            val duplicateAccount = AccountInput(Username("username2"), Password("p"), address)
            errCreateAccount(duplicateAccount) shouldBe EmailAddressTakenException.message
        }

        test("An account with a disallowed email address domain shouldn't be created") {
            errCreateAccount(AccountInput(Username("u"), Password("p"), "bob@outlook.com")) shouldBe
                    InvalidDomainException.message
        }
    }

    context("createContacts(DataFetchingEnvironment)") {
        test("Trying to save the user's own contact should be ignored") {
            val (ownerId, userId) = createVerifiedUsers(2).map { it.info.id }
            createContacts(ownerId, listOf(ownerId, userId))
            Contacts.readIdList(ownerId) shouldBe listOf(userId)
        }

        test("If one of the contacts to be saved is invalid, then none of them should be saved") {
            val (ownerId, userId) = createVerifiedUsers(2).map { it.info.id }
            val contacts = listOf(userId, -1)
            errCreateContacts(ownerId, contacts) shouldBe InvalidContactException.message
            Contacts.readIdList(ownerId).shouldBeEmpty()
        }
    }

    context("createGroupChat(DataFetchingEnvironment)") {
        test("A group chat should be created automatically including the creator as a user and admin") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf(user1Id, user2Id),
                "adminIdList" to listOf<Int>(),
                "isBroadcast" to false,
                "isPublic" to false,
                "isInvitable" to false
            )
            val chatId = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), adminId)
                .data!!["createGroupChat"] as Int
            val chats = GroupChats.readUserChats(adminId)
            chats shouldHaveSize 1
            with(chats[0]) {
                id shouldBe chatId
                users.edges.map { it.node.id } shouldContainExactlyInAnyOrder listOf(adminId, user1Id, user2Id)
                adminIdList shouldBe listOf(adminId)
            }
        }

        test("A public chat should be made invitable server-side even if the client stated it shouldn't be") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf<Int>(),
                "adminIdList" to listOf<Int>(),
                "isBroadcast" to false,
                "isPublic" to true,
                "isInvitable" to false
            )
            val chatId = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), adminId)
                .data!!["createGroupChat"] as Int
            with(GroupChats.readChat(chatId)) {
                isPublic.shouldBeTrue()
                isInvitable.shouldBeTrue()
            }
        }

        test("A group chat shouldn't be created when supplied with an invalid user ID") {
            val userId = createVerifiedUsers(1)[0].info.id
            val invalidUserId = -1
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(userId, invalidUserId),
                adminIdList = listOf(userId),
                isBroadcast = false,
                isPublic = false,
                isInvitable = false
            )
            errCreateGroupChat(userId, chat) shouldBe InvalidUserIdException.message
        }

        test("A group chat shouldn't be created if the admin ID list isn't a subset of the user ID list") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chat = mapOf(
                "title" to "Title",
                "description" to "description",
                "userIdList" to listOf<Int>(),
                "adminIdList" to listOf(user2Id),
                "isBroadcast" to false,
                "isPublic" to false,
                "isInvitable" to false
            )
            executeGraphQlViaEngine(
                CREATE_GROUP_CHAT_QUERY,
                mapOf("chat" to chat),
                user1Id
            ).errors!![0].message shouldBe InvalidAdminIdException.message
        }
    }

    context("createTextMessage(DataFetchingEnvironment)") {
        test("The user should be able to create a message in a private chat they just deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            createTextMessage(user1Id, chatId, MessageText("t"))
        }

        test("Messaging in a chat the user isn't in should throw an exception") {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            GroupChats.create(listOf(admin2Id))
            errCreateTextMessage(admin2Id, chatId, MessageText("t")) shouldBe InvalidChatIdException.message
        }

        test("The message should be created sans context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            createTextMessage(adminId, chatId, MessageText("t"))
            Messages.readGroupChat(chatId, userId = adminId).map { it.node.context } shouldBe
                    listOf(MessageContext(hasContext = false, id = null))
        }

        test("The message should be created with a context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            createTextMessage(adminId, chatId, MessageText("t"), contextMessageId = messageId)
            Messages.readGroupChat(chatId, userId = adminId)[1].node.context shouldBe
                    MessageContext(hasContext = true, id = messageId)
        }

        test("Using a nonexistent message context should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            errCreateTextMessage(adminId, chatId, MessageText("t"), contextMessageId = 1) shouldBe
                    InvalidMessageIdException.message
        }

        test("A non-admin shouldn't be allowed to message in a broadcast chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            executeGraphQlViaHttp(
                CREATE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "text" to "Hi"),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }
    }

    context("createPrivateChat(DataFetchingEnvironment)") {
        test("A chat should be created") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = createPrivateChat(user1Id, user2Id)
            PrivateChats.readIdList(user1Id) shouldBe listOf(chatId)
        }

        test("Attempting to create a chat the user is in should return an error") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            createPrivateChat(user1Id, user2Id)
            errCreatePrivateChat(user1Id, user2Id) shouldBe ChatExistsException.message
        }

        test(
            """
            Given a chat which was deleted by the user,
            when the user recreates the chat,
            then the existing chat's ID should be received
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = createPrivateChat(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            createPrivateChat(user1Id, user2Id) shouldBe chatId
        }

        test("A chat shouldn't be created with a nonexistent user") {
            val userId = createVerifiedUsers(1)[0].info.id
            errCreatePrivateChat(userId, otherUserId = -1) shouldBe InvalidUserIdException.message
        }

        test("A chat shouldn't be created with the user themselves") {
            val userId = createVerifiedUsers(1)[0].info.id
            errCreatePrivateChat(userId, userId) shouldBe InvalidUserIdException.message
        }
    }

    context("createStatus(DataFetchingEnvironment") {
        /** A private chat between two users where [user2Id] sent the [messageId]. */
        data class UtilizedPrivateChat(val messageId: Int, val user1Id: Int, val user2Id: Int)

        fun createUtilizedPrivateChat(): UtilizedPrivateChat {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            return UtilizedPrivateChat(messageId, user1Id, user2Id)
        }

        test("A status should be created") {
            val (messageId, user1Id) = createUtilizedPrivateChat()
            createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            val statuses = MessageStatuses.read(messageId)
            statuses shouldHaveSize 1
            statuses[0].status shouldBe MessageStatus.DELIVERED
        }

        test("Creating a duplicate status should fail") {
            val (messageId, user1Id) = createUtilizedPrivateChat()
            createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            errCreateStatus(user1Id, messageId, MessageStatus.DELIVERED) shouldBe DuplicateStatusException.message
        }

        test("Creating a status on the user's own message should fail") {
            val (messageId, _, user2Id) = createUtilizedPrivateChat()
            errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED) shouldBe InvalidMessageIdException.message
        }

        test("Creating a status on a message from a chat the user isn't in should fail") {
            val (messageId) = createUtilizedPrivateChat()
            val userId = createVerifiedUsers(1)[0].info.id
            errCreateStatus(userId, messageId, MessageStatus.DELIVERED) shouldBe InvalidMessageIdException.message
        }

        test("Creating a status on a nonexistent message should fail") {
            val userId = createVerifiedUsers(1)[0].info.id
            errCreateStatus(userId, messageId = 1, status = MessageStatus.DELIVERED) shouldBe
                    InvalidMessageIdException.message
        }

        test("Creating a status in a private chat the user deleted should fail") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            errCreateStatus(user1Id, messageId, MessageStatus.DELIVERED) shouldBe InvalidMessageIdException.message
        }

        test(
            """
            Given a private chat in which the first user sent a message, and the second user deleted the chat,
            when the second user creates a status on the message,
            then it should fail
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED) shouldBe InvalidMessageIdException.message
        }
    }

    context("deleteAccount(DataFetchingEnvironment)") {
        test("An account should be deleted from the auth system") {
            val userId = createVerifiedUsers(1)[0].info.id
            deleteAccount(userId)
            Users.exists(userId).shouldBeFalse()
        }

        test("An account shouldn't be deleted if the user is the last admin of a group chat with other users") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            errDeleteAccount(adminId) shouldBe CannotDeleteAccountException.message
        }
    }

    context("deleteContacts(DataFetchingEnvironment)") {
        test("Contacts should be deleted, ignoring invalid ones") {
            val (ownerId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            Contacts.create(ownerId, userIdList.toSet())
            deleteContacts(ownerId, userIdList + -1)
            Contacts.readIdList(ownerId).shouldBeEmpty()
        }
    }

    context("deleteMessage(DataFetchingEnvironment)") {
        test("The user's message should be deleted") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            deleteMessage(adminId, messageId)
            Messages.readGroupChat(chatId, userId = adminId).shouldBeEmpty()
        }

        test("Deleting a nonexistent message should return an error") {
            val userId = createVerifiedUsers(1)[0].info.id
            errDeleteMessage(userId, messageId = 0) shouldBe InvalidMessageIdException.message
        }

        test("Deleting a message from a chat the user isn't in should throw an exception") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            errDeleteMessage(userId, messageId) shouldBe InvalidMessageIdException.message
        }

        test("Deleting another user's message should return an error") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            errDeleteMessage(user1Id, messageId) shouldBe InvalidMessageIdException.message
        }

        test(
            """
            Given a user who created a private chat, sent a message, and deleted the chat,
            when deleting the message,
            then it should fail
            """
        ) {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            errDeleteMessage(user1Id, messageId) shouldBe InvalidMessageIdException.message
        }
    }

    context("deletePrivateChat(DataFetchingEnvironment)") {
        test("A chat should be deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            deletePrivateChat(user1Id, chatId)
            PrivateChatDeletions.isDeleted(user1Id, chatId).shouldBeTrue()
        }

        test("Deleting an invalid chat ID should throw an exception") {
            val userId = createVerifiedUsers(1)[0].info.id
            errDeletePrivateChat(userId, chatId = 1) shouldBe InvalidChatIdException.message
        }
    }

    context("resetPassword(DataFetchingEnvironment") {
        test("A password reset request should be sent") {
            val address = createVerifiedUsers(1)[0].info.emailAddress
            resetPassword(address)
        }

        test("Requesting a password reset for an unregistered address should throw an exception") {
            errResetPassword("username@example.com") shouldBe UnregisteredEmailAddressException.message
        }
    }

    context("sendEmailAddressVerification(DataFetchingEnvironment)") {
        test("A verification email should be sent") {
            val address = "username@example.com"
            val account = AccountInput(Username("username"), Password("password"), address)
            createUser(account)
            sendEmailAddressVerification(address)
        }

        test("Sending a verification email to an unregistered address should throw an exception") {
            errSendEmailVerification("username@example.com") shouldBe UnregisteredEmailAddressException.message
        }
    }

    context("updateAccount(DataFetchingEnvironment)") {
        fun testAccount(accountBeforeUpdate: Account, accountAfterUpdate: AccountUpdate) {
            isUsernameTaken(accountBeforeUpdate.username).shouldBeFalse()
            with(readUserByUsername(accountAfterUpdate.username!!)) {
                username shouldBe accountAfterUpdate.username
                emailAddress shouldBe accountAfterUpdate.emailAddress
                isEmailVerified(id).shouldBeFalse()
                firstName shouldBe accountBeforeUpdate.firstName
                lastName shouldBe accountAfterUpdate.lastName
                bio shouldBe accountBeforeUpdate.bio
            }
        }

        test("Only the specified fields should be updated") {
            val user = createVerifiedUsers(1)[0].info
            val update =
                AccountUpdate(Username("john_roger"), emailAddress = "john.roger@example.com", lastName = "Roger")
            updateAccount(user.id, update)
            testAccount(user, update)
        }

        test("Updating a username to one already taken shouldn't allow the account to be updated") {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            errUpdateAccount(user1.id, AccountUpdate(username = user2.username)) shouldBe UsernameTakenException.message
        }

        test("Updating an email to one already taken shouldn't allow the account to be updated") {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            errUpdateAccount(user1.id, AccountUpdate(emailAddress = user2.emailAddress)) shouldBe
                    EmailAddressTakenException.message
        }
    }
})
