package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.awaitBrokering
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.messagesNotifier
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.graphql.routing.*
import io.ktor.http.*
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.test.*

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

@ExtendWith(DbExtension::class)
class MutationsTest {
    @Nested
    inner class UnblockUser {
        @Test
        fun `The user must be unblocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            BlockedUsers.create(blockerId, blockedId)
            assertTrue(unblockUser(blockerId, blockedId))
            assertEquals(0, BlockedUsers.count())
        }

        @Test
        fun `Unblocking a user who wasn't blocked must return 'false'`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            assertFalse(unblockUser(user1Id, user2Id))
        }

        @Test
        fun `Unblocking a user who doesn't exist must return 'false'`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertFalse(unblockUser(userId, blockedUserId = -1))
        }
    }

    @Nested
    inner class BlockUser {
        @Test
        fun `'null' must be returned once the user gets blocked`() {
            val (blockerId, blockedId) = createVerifiedUsers(2).map { it.info.id }
            assertNull(blockUser(blockerId, blockedId))
            val userId = BlockedUsers.read(blockerId).edges[0].node.id
            assertEquals(blockedId, userId)
        }

        @Test
        fun `A nonexistent user mustn't be blocked`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(blockUser(userId, -1) is InvalidUserId)
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
            assertNull(resetPassword(user.emailAddress, user.passwordResetCode, password))
            val login = Login(account.username, password)
            assertTrue(Users.isValidLogin(login))
        }

        @Test
        fun `Using an invalid code mustn't reset the password`() {
            val account = AccountInput(Username("username"), Password("p"), "john@example.com")
            Users.create(account)
            val password = Password("new")
            assertTrue(resetPassword(account.emailAddress, 123, password) is InvalidPasswordResetCode)
            val login = Login(account.username, password)
            assertFalse(Users.isValidLogin(login))
        }

        @Test
        fun `Resetting the password for an unregistered email address must fail`() {
            val result = resetPassword("john@example.com", 123, Password("new"))
            assertTrue(result is UnregisteredEmailAddress)
        }
    }

    @Nested
    inner class VerifyEmailAddress {
        @Test
        fun `The email address must get verified`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            val user = Users.read(account.username)
            assertNull(verifyEmailAddress(user.emailAddress, user.emailAddressVerificationCode))
            assertTrue(Users.read(account.username).hasVerifiedEmailAddress)
        }

        @Test
        fun `Using an invalid code mustn't verify the email address`() {
            val account = AccountInput(Username("username"), Password("p"), "john.doe@example.com")
            Users.create(account)
            assertTrue(verifyEmailAddress(account.emailAddress, 123) is InvalidVerificationCode)
            assertFalse(Users.read(account.username).hasVerifiedEmailAddress)
        }

        @Test
        fun `Attempting to verify an email address which isn't associated with an account must fail`(): Unit =
            assertTrue(verifyEmailAddress("john.doe@example.com", 123) is UnregisteredEmailAddress)
    }

    @Nested
    inner class TriggerAction {
        @Test
        fun `The action must be triggered`(): Unit = runBlocking {
            val admin = createVerifiedUsers(1).first().info
            val chatId = GroupChats.create(listOf(admin.id))
            val action = MessageText("Yes")
            val messageId = Messages.message(
                admin.id,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(action, MessageText("No"))),
            )
            awaitBrokering()
            val subscriber = messagesNotifier.subscribe(admin.id).subscribeWith(TestSubscriber())
            assertNull(triggerAction(admin.id, messageId, action))
            awaitBrokering()
            subscriber.assertValue(TriggeredAction(messageId, action, admin))
        }

        @Test
        fun `Triggering a message which isn't an action message must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val result = triggerAction(adminId, messageId, MessageText("action"))
            assertTrue(result is InvalidMessageId)
        }

        @Test
        fun `Triggering a nonexistent action must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No"))),
            )
            val result = triggerAction(adminId, messageId, MessageText("action"))
            assertTrue(result is InvalidAction)
        }
    }

    @Nested
    inner class CreateActionMessage {
        @Test
        fun `The message must be created with the context`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val message = ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No")))
            assertNull(createActionMessage(adminId, chatId, message, contextMessageId))
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
                        listOf(MessageText("Yes"), MessageText("No")),
                    ),
                ),
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Messaging in a chat the user isn't in must fail`() {
            val userId = createVerifiedUsers(1).first().info.id
            val result = createActionMessage(
                userId,
                chatId = 1,
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No"))),
            )
            assertTrue(result is InvalidChatId)
        }

        @Test
        fun `Supplying an invalid action message must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val data = executeGraphQlViaEngine(
                CREATE_ACTION_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "message" to mapOf("text" to "Do you code?", "actions" to listOf<String>())),
                adminId,
            ).data!!["createActionMessage"]!!
            assertTrue(testingObjectMapper.convertValue<CreateActionMessageResult>(data) is InvalidAction)
        }

        @Test
        fun `Using a nonexistent context message ID must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val result = createActionMessage(
                adminId,
                chatId,
                ActionMessageInput(MessageText("Do you code?"), listOf(MessageText("Yes"), MessageText("No"))),
                contextMessageId = 1,
            )
            assertTrue(result is InvalidMessageId)
        }
    }

    @Nested
    inner class JoinPublicChat {
        @Test
        fun `The public chat must be joined`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertNull(joinPublicChat(userId, chatId))
            assertEquals(chatId, GroupChats.readUserChats(userId).first().id)
        }

        @Test
        fun `A non-public chat mustn't be joined`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            assertTrue(joinPublicChat(userId, chatId) is InvalidChatId)
        }
    }

    @Nested
    inner class LeaveGroupChat {
        @Test
        fun `The user mustn't be able to leave a chat they aren't in`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            assertTrue(leaveGroupChat(userId, chatId) is InvalidChatId)
        }

        @Test
        fun `The last admin of an otherwise nonempty chat mustn't be able to leave`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertTrue(leaveGroupChat(adminId, chatId) is CannotLeaveChat)
        }

        @Test
        fun `The user must be able to leave the chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            assertNull(leaveGroupChat(userId, chatId))
        }
    }

    @Nested
    inner class ForwardMessage {
        @Test
        fun `The message must be forwarded with a context`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val (chat1Id, chat2Id) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val messageId = Messages.message(adminId, chat1Id)
            val contextMessageId = Messages.message(adminId, chat2Id)
            assertNull(forwardMessage(adminId, chat2Id, messageId, contextMessageId))
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
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Messaging in a chat the user isn't in must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            assertTrue(forwardMessage(admin1Id, chat2Id, messageId) is InvalidChatId)
        }

        @Test
        fun `Forwarding a nonexistent message must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertTrue(forwardMessage(adminId, chatId, messageId = 1) is InvalidMessageId)
        }

        @Test
        fun `Using an invalid context message must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val result = forwardMessage(adminId, chatId, messageId, contextMessageId = 1)
            assertTrue(result is InvalidMessageId)
        }

        @Test
        fun `Forwarding a message the user can't see must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val (chat1Id, chat2Id) = listOf(admin1Id, admin2Id).map { GroupChats.create(listOf(it)) }
            val messageId = Messages.message(admin1Id, chat1Id)
            assertTrue(forwardMessage(admin2Id, chat2Id, messageId) is InvalidMessageId)
        }
    }

    @Nested
    inner class SetInvitability {
        @Test
        fun `The chat's invitability must be updated`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertNull(setInvitability(adminId, chatId, isInvitable = true))
            assertEquals(GroupChatPublicity.INVITABLE, GroupChats.readChat(chatId).publicity)
        }

        @Test
        fun `Updating a public chat must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertTrue(setInvitability(adminId, chatId, isInvitable = true) is InvalidChatId)
        }

        @Test
        fun `An error must be returned when a non-admin updates the invitability`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                SET_INVITABILITY_QUERY,
                mapOf("chatId" to chatId, "isInvitable" to true),
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class CreateGroupChatInviteMessage {
        @Test
        fun `A message must be created with a context`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val (chatId, invitedChatId) = listOf(1, 2)
                .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val contextMessageId = Messages.message(adminId, chatId)
            assertNull(createGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId))
            assertEquals(1, GroupChatInviteMessages.count())
        }

        @Test
        fun `Messaging in a broadcast chat must fail`() {
            val (admin1, admin2) = createVerifiedUsers(2)
            val chatId = GroupChats.create(
                adminIdList = listOf(admin1.info.id),
                userIdList = listOf(admin2.info.id),
                isBroadcast = true,
            )
            val invitedChatId = GroupChats.create(listOf(admin2.info.id), publicity = GroupChatPublicity.INVITABLE)
            val response = executeGraphQlViaHttp(
                CREATE_GROUP_CHAT_INVITE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "invitedChatId" to invitedChatId),
                admin2.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `Creating a message in a chat the user isn't in must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val invitedChatId = GroupChats.create(listOf(adminId))
            val result = createGroupChatInviteMessage(adminId, chatId = 1, invitedChatId = invitedChatId)
            assertTrue(result is InvalidChatId)
        }

        @Test
        fun `Inviting users to a private chat must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminIdList = listOf(user1Id))
            val invitedChatId = PrivateChats.create(user1Id, user2Id)
            val result = createGroupChatInviteMessage(user1Id, chatId, invitedChatId)
            assertTrue(result is InvalidInvitedChat)
        }

        @Test
        fun `Inviting users to a group chat with invites turned off must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val (chatId, invitedChatId) = listOf(1, 2).map { GroupChats.create(listOf(adminId)) }
            val result = createGroupChatInviteMessage(adminId, chatId, invitedChatId)
            assertTrue(result is InvalidInvitedChat)
        }

        @Test
        fun `Using an invalid content message must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val (chatId, invitedChatId) = listOf(1, 2)
                .map { GroupChats.create(listOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val result = createGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId = 1)
            assertTrue(result is InvalidMessageId)
        }
    }

    @Nested
    inner class JoinGroupChat {
        @Test
        fun `An invite code must be used to join the chat, even if the chat has already been joined`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            repeat(2) {
                val result = joinGroupChat(userId, GroupChats.readInviteCode(chatId))
                assertNull(result)
            }
            assertEquals(setOf(adminId, userId), GroupChatUsers.readUserIdList(chatId).toSet())
        }

        @Test
        fun `Using an invalid invite code must fail`() {
            val userId = createVerifiedUsers(1).first().info.id
            val result = joinGroupChat(userId, inviteCode = UUID.randomUUID())
            assertTrue(result is InvalidInviteCode)
        }
    }

    @Nested
    inner class CreatePollMessage {
        @Test
        fun `A poll message must be created with a context`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            assertNull(createPollMessage(adminId, chatId, poll, contextMessageId))
            val message = Messages.readGroupChat(chatId, userId = adminId).last().node
            assertEquals(contextMessageId, message.context.id)
            val options = poll.options.map { PollOption(it, votes = listOf()) }
            assertEquals(Poll(poll.title, options), PollMessages.read(message.messageId))
        }

        @Test
        fun `Messaging a poll in a chat the user isn't in must fail`() {
            val userId = createVerifiedUsers(1).first().info.id
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            assertTrue(createPollMessage(userId, chatId = 1, poll = poll) is InvalidChatId)
        }

        @Test
        fun `Creating a poll in response to a nonexistent message must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val result = createPollMessage(adminId, chatId, poll, contextMessageId = 1)
            assertTrue(result is InvalidMessageId)
        }

        @Test
        fun `Using an invalid poll must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = mapOf("title" to "Title", "options" to listOf("option"))
            val data = executeGraphQlViaEngine(
                CREATE_POLL_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to null),
                adminId,
            ).data!!["createPollMessage"]!!
            assertTrue(testingObjectMapper.convertValue<CreatePollMessageResult>(data) is InvalidPoll)
        }
    }

    @Nested
    inner class SetPollVote {
        @Test
        fun `The user's vote must be updated`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val option = MessageText("option 1")
            val poll = PollInput(MessageText("Title"), listOf(option, MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            assertNull(setPollVote(adminId, messageId, option, vote = true))
            assertEquals(listOf(adminId), PollMessages.read(messageId).options.first { it.option == option }.votes)
        }

        @Test
        fun `Voting on a message which isn't a poll must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            val result = setPollVote(adminId, messageId, MessageText("option"), vote = true)
            assertTrue(result is InvalidMessageId)
        }

        @Test
        fun `Voting on a nonexistent poll must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val result = setPollVote(adminId, messageId = 1, option = MessageText("option"), vote = true)
            assertTrue(result is InvalidMessageId)
        }

        @Test
        fun `Voting for a nonexistent option must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val poll = PollInput(MessageText("Title"), listOf(MessageText("option 1"), MessageText("option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val result = setPollVote(adminId, messageId, MessageText("nonexistent option"), vote = true)
            assertTrue(result is NonexistentOption)
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
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }

        @Test
        fun `The broadcast status must be updated`() {
            val adminId = createVerifiedUsers(1).first().info.id
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
        fun `Making a user who isn't in the chat an admin mustn't fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            makeGroupChatAdmins(adminId, chatId, listOf(userId))
        }

        @Test
        fun `A non-admin mustn't be allowed to make users admins`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                MAKE_GROUP_CHAT_ADMINS_QUERY,
                mapOf("chatId" to chatId, "idList" to listOf<Int>()),
                user.accessToken,
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
            assertEquals(setOf(adminId, userId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `Adding a nonexistent user must do nothing`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val invalidUserId = -1
            addGroupChatUsers(adminId, chatId, listOf(invalidUserId))
            assertEquals(setOf(adminId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `A non-admin mustn't be allowed to update the chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            val response = executeGraphQlViaHttp(
                ADD_GROUP_CHAT_USERS_QUERY,
                mapOf("chatId" to chatId, "idList" to listOf<Int>()),
                user.accessToken,
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
            val result = removeGroupChatUsers(admin1Id, chatId, listOf(admin1Id, userId))
            assertNull(result)
            assertEquals(listOf(admin2Id), GroupChats.readChat(chatId).users.edges.map { it.node.id })
        }

        @Test
        fun `Removing the last admin must be allowed if there won't be any remaining users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val result = removeGroupChatUsers(adminId, chatId, listOf(adminId, userId))
            assertNull(result)
            assertEquals(0, GroupChats.count())
        }

        @Test
        fun `Removing the last admin mustn't be allowed if there are other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            val result = removeGroupChatUsers(adminId, chatId, listOf(adminId))
            assertTrue(result is CannotLeaveChat)
        }

        @Test
        fun `Removing invalid users mustn't fail`() {
            val (adminId, userNotInChatId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val result = removeGroupChatUsers(adminId, chatId, listOf(-1, userNotInChatId))
            assertNull(result)
        }
    }

    @Nested
    inner class UpdateGroupChatDescription {
        @Test
        fun `The admin must update the description`() {
            val adminId = createVerifiedUsers(1).first().info.id
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
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class UpdateGroupChatTitle {
        @Test
        fun `The admin must update the title`() {
            val adminId = createVerifiedUsers(1).first().info.id
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
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class Unstar {
        @Test
        fun `A message must be starred`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            unstar(adminId, messageId)
            assertFalse(Stargazers.hasStar(adminId, messageId))
        }
    }

    @Nested
    inner class Star {
        @Test
        fun `A message must be starred`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertNull(star(adminId, messageId))
            assertEquals(listOf(messageId), Stargazers.read(adminId).edges.map { it.node.messageId })
        }

        @Test
        fun `Starring a message from a chat the user isn't in must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            val messageId = Messages.message(admin1Id, chatId)
            assertTrue(star(admin2Id, messageId) is InvalidMessageId)
        }
    }

    @Nested
    inner class SetOnline {
        private fun assertOnlineStatus(isOnline: Boolean) {
            val userId = createVerifiedUsers(1).first().info.id
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
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            assertNull(setTyping(adminId, chatId, isTyping))
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
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(setTyping(userId, chatId = 1, isTyping = true) is InvalidChatId)
        }
    }

    @Nested
    inner class DeleteGroupChatPic {
        @Test
        fun `Deleting the pic must remove it`() {
            val adminId = createVerifiedUsers(1).first().info.id
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
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class DeleteProfilePic {
        @Test
        fun `The user's profile pic must be deleted`() {
            val userId = createVerifiedUsers(1).first().info.id
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
            assertNull(createAccount(account))
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
            assertTrue(createAccount(account) is UsernameTaken)
        }

        @Test
        fun `An account with a taken email mustn't be created`() {
            val address = "username@example.com"
            val account = AccountInput(Username("username1"), Password("p"), address)
            createAccount(account)
            val duplicateAccount = AccountInput(Username("username2"), Password("p"), address)
            assertTrue(createAccount(duplicateAccount) is EmailAddressTaken)
        }

        @Test
        fun `An account with a disallowed email address domain mustn't be created`() {
            val response = createAccount(AccountInput(Username("u"), Password("p"), "bob@outlook.com"))
            assertTrue(response is InvalidDomain)
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
                "publicity" to GroupChatPublicity.NOT_INVITABLE,
            )
            val data = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), adminId)
                .data!!["createGroupChat"]!!
            val chatId = testingObjectMapper.convertValue<CreatedChatId>(data).id
            val chats = GroupChats.readUserChats(adminId)
            assertEquals(1, chats.size)
            assertEquals(chatId, chats.first().id)
            val participants = chats.first().users.edges.map { it.node.id }.toSet()
            assertEquals(setOf(adminId, user1Id, user2Id), participants)
            assertEquals(listOf(adminId), chats.first().adminIdList)
        }

        private fun create(adminId: Int, userIdList: List<Int>) {
            val chat = mapOf(
                "title" to GroupChatTitle("T"),
                "description" to GroupChatDescription(""),
                "userIdList" to userIdList,
                "adminIdList" to listOf(adminId),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE,
            )
            val response = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), adminId)
                .data!!["createGroupChat"]!!
            assertTrue(testingObjectMapper.convertValue<CreateGroupChatResult>(response) is CreatedChatId)
        }

        @Test
        fun `A group chat must be created when supplied with an invalid user ID`() {
            val adminId = createVerifiedUsers(1).first().info.id
            create(adminId, userIdList = listOf(adminId, -1))
        }

        @Test
        fun `The chat must be created if the user's ID was passed in the admin ID list but not user ID list`() {
            val adminId = createVerifiedUsers(1).first().info.id
            create(adminId, userIdList = listOf())
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
                "publicity" to GroupChatPublicity.NOT_INVITABLE,
            )
            val response = executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), user1Id)
                .data!!["createGroupChat"]!!
            assertTrue(testingObjectMapper.convertValue<CreateGroupChatResult>(response) is InvalidAdminId)
        }
    }

    @Nested
    inner class CreateTextMessage {
        @Test
        fun `The user must be able to create a message in a private chat they just deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            val result = createTextMessage(user1Id, chatId, MessageText("t"))
            assertNull(result)
        }

        @Test
        fun `Messaging in a chat the user isn't in must throw an exception`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            GroupChats.create(listOf(admin2Id))
            val result = createTextMessage(admin2Id, chatId, MessageText("t"))
            assertTrue(result is InvalidChatId)
        }

        @Test
        fun `The message must be created sans context`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            createTextMessage(adminId, chatId, MessageText("t"))
            val contexts = Messages.readGroupChat(chatId, userId = adminId).map { it.node.context }
            assertEquals(listOf(MessageContext(hasContext = false, id = null)), contexts)
        }

        @Test
        fun `The message must be created with a context`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            createTextMessage(adminId, chatId, MessageText("t"), contextMessageId = messageId)
            val context = Messages.readGroupChat(chatId, userId = adminId).last().node.context
            assertEquals(MessageContext(hasContext = true, id = messageId), context)
        }

        @Test
        fun `Using a nonexistent message context must fail`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val response = createTextMessage(adminId, chatId, MessageText("t"), contextMessageId = 1)
            assertTrue(response is InvalidMessageId)
        }

        @Test
        fun `A non-admin mustn't be allowed to message in a broadcast chat`() {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id), isBroadcast = true)
            val response = executeGraphQlViaHttp(
                CREATE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "text" to "Hi"),
                user.accessToken,
            )
            assertEquals(HttpStatusCode.Unauthorized, response.status())
        }
    }

    @Nested
    inner class CreatePrivateChat {
        @Test
        fun `A chat must be created`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val createdChatId = createPrivateChat(user1Id, user2Id) as CreatedChatId
            assertEquals(setOf(createdChatId.id), PrivateChats.readIdList(user1Id))
        }

        @Test
        fun `Recreating a chat the user deleted must cause the existing chat's ID to be returned`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val createdChatId = createPrivateChat(user1Id, user2Id) as CreatedChatId
            PrivateChatDeletions.create(createdChatId.id, user1Id)
            assertEquals(createdChatId, createPrivateChat(user1Id, user2Id))
        }

        @Test
        fun `A chat mustn't be created with a nonexistent user`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(createPrivateChat(userId, otherUserId = -1) is InvalidUserId)
        }

        @Test
        fun `A chat mustn't be created with the user themselves`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(createPrivateChat(userId, userId) is InvalidUserId)
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
            assertNull(createStatus(user1Id, messageId, MessageStatus.DELIVERED))
            val statuses = MessageStatuses.read(messageId)
            assertEquals(1, statuses.size)
            assertEquals(MessageStatus.DELIVERED, statuses.first().status)
        }

        @Test
        fun `Creating a duplicate status must do nothing`(): Unit = runBlocking {
            val (messageId, userId) = createUtilizedPrivateChat()
            val create = { createStatus(userId, messageId, MessageStatus.DELIVERED) }
            create()
            awaitBrokering()
            val subscriber = messagesNotifier.subscribe(userId).subscribeWith(TestSubscriber())
            create()
            awaitBrokering()
            subscriber.assertNoValues()
        }

        @Test
        fun `Creating a status on the user's own message must fail`() {
            val (messageId, _, user2Id) = createUtilizedPrivateChat()
            assertTrue(createStatus(user2Id, messageId, MessageStatus.DELIVERED) is InvalidMessageId)
        }

        @Test
        fun `Creating a status on a message from a chat the user isn't in must fail`() {
            val (messageId) = createUtilizedPrivateChat()
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(createStatus(userId, messageId, MessageStatus.DELIVERED) is InvalidMessageId)
        }

        @Test
        fun `Creating a status on a nonexistent message must fail`() {
            val userId = createVerifiedUsers(1).first().info.id
            val response = createStatus(userId, messageId = 1, status = MessageStatus.DELIVERED)
            assertTrue(response is InvalidMessageId)
        }

        @Test
        fun `Creating a status in a private chat the user deleted must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val result = createStatus(user1Id, messageId, MessageStatus.DELIVERED)
            assertTrue(result is InvalidMessageId)
        }

        @Test
        fun `Creating a status on a message which was sent before the user deleted the private chat must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user2Id)
            val response = createStatus(user2Id, messageId, MessageStatus.DELIVERED)
            assertTrue(response is InvalidMessageId)
        }
    }

    @Nested
    inner class DeleteAccount {
        @Test
        fun `'null' must be returned when an account gets deleted from the auth system`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertNull(deleteAccount(userId))
            assertFalse(Users.isExisting(userId))
        }

        @Test
        fun `An account mustn't be deleted if the user is the last admin of a group chat with other users`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(listOf(adminId), listOf(userId))
            assertTrue(deleteAccount(adminId) is CannotDeleteAccount)
        }
    }

    @Nested
    inner class CreateContact {
        @Test
        fun `The contact must be saved`() {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            assertTrue(createContact(ownerId, contactId))
            assertEquals(setOf(contactId), Contacts.readIdList(ownerId))
        }

        @Test
        fun `A nonexistent user mustn't be saved as a contact`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertFalse(createContact(userId, id = -1))
        }

        @Test
        fun `A previously saved contact mustn't be saved`() {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            createContact(ownerId, contactId)
            assertFalse(createContact(ownerId, contactId))
        }
    }

    @Nested
    inner class DeleteContact {
        @Test
        fun `The contact must be deleted`() {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            Contacts.create(ownerId, contactId)
            assertTrue(deleteContact(ownerId, contactId))
            assertTrue(Contacts.readIdList(ownerId).isEmpty())
        }

        @Test
        fun `Attempting to delete a nonexistent user from the user's contacts must return 'false'`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertFalse(deleteContact(userId, id = -1))
        }

        @Test
        fun `Attempting to delete a contact which isn't saved must return 'false'`() {
            val (ownerId, contactId) = createVerifiedUsers(2).map { it.info.id }
            assertFalse(deleteContact(ownerId, contactId))
        }
    }

    @Nested
    inner class DeleteMessage {
        @Test
        fun `The user's message must be deleted`() {
            val adminId = createVerifiedUsers(1).first().info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertNull(deleteMessage(adminId, messageId))
            assertTrue(Messages.readGroupChat(chatId, userId = adminId).isEmpty())
        }

        @Test
        fun `Deleting a nonexistent message must return an error`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(deleteMessage(userId, messageId = 0) is InvalidMessageId)
        }

        @Test
        fun `Deleting a message from a chat the user isn't in must throw an exception`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertTrue(deleteMessage(userId, messageId) is InvalidMessageId)
        }

        @Test
        fun `Deleting another user's message must return an error`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user2Id, chatId)
            assertTrue(deleteMessage(user1Id, messageId) is InvalidMessageId)
        }

        @Test
        fun `Deleting a message sent before the private chat was deleted by the user must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertTrue(deleteMessage(user1Id, messageId) is InvalidMessageId)
        }
    }

    @Nested
    inner class DeletePrivateChat {
        @Test
        fun `A chat must be deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertNull(deletePrivateChat(user1Id, chatId))
            assertTrue(PrivateChatDeletions.isDeleted(user1Id, chatId))
        }

        @Test
        fun `Deleting an invalid chat ID must throw an exception`() {
            val userId = createVerifiedUsers(1).first().info.id
            assertTrue(deletePrivateChat(userId, chatId = 1) is InvalidChatId)
        }
    }

    @Nested
    inner class EmailPasswordResetCode {
        @Test
        fun `A password reset request must be sent`() {
            val address = createVerifiedUsers(1).first().info.emailAddress
            assertNull(emailPasswordResetCode(address))
        }

        @Test
        fun `Requesting a password reset for an unregistered address must throw an exception`(): Unit =
            assertTrue(emailPasswordResetCode("username@example.com") is UnregisteredEmailAddress)
    }

    @Nested
    inner class EmailEmailAddressVerification {
        @Test
        fun `A verification email must be sent`() {
            val address = "username@example.com"
            val account = AccountInput(Username("u"), Password("p"), address)
            Users.create(account)
            assertNull(emailEmailAddressVerification(address))
        }

        @Test
        fun `Sending a verification email to an unregistered address must throw an exception`(): Unit =
            assertTrue(emailEmailAddressVerification("username@example.com") is UnregisteredEmailAddress)

        @Test
        fun `Sending a verification email to a verified address must fail`() {
            val address = createVerifiedUsers(1).first().info.emailAddress
            assertTrue(emailEmailAddressVerification(address) is EmailAddressVerified)
        }
    }

    @Nested
    inner class UpdateAccount {
        private fun testAccount(accountBeforeUpdate: Account, accountAfterUpdate: AccountUpdate) {
            assertFalse(Users.isUsernameTaken(accountBeforeUpdate.username))
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
            val user = createVerifiedUsers(1).first().info
            val update =
                AccountUpdate(Username("john_roger"), emailAddress = "john.roger@example.com", lastName = Name("Roger"))
            assertNull(updateAccount(user.id, update))
            testAccount(user, update)
        }

        @Test
        fun `Updating a username to one already taken mustn't allow the account to be updated`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val response = updateAccount(user1.id, AccountUpdate(username = user2.username))
            assertTrue(response is UsernameTaken)
        }

        @Test
        fun `Updating an email to one already taken mustn't allow the account to be updated`() {
            val (user1, user2) = createVerifiedUsers(2).map { it.info }
            val response = updateAccount(user1.id, AccountUpdate(emailAddress = user2.emailAddress))
            assertTrue(response is EmailAddressTaken)
        }
    }
}
