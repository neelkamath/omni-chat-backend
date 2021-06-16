package com.neelkamath.omniChatBackend.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChatBackend.*
import com.neelkamath.omniChatBackend.db.*
import com.neelkamath.omniChatBackend.db.tables.*
import com.neelkamath.omniChatBackend.graphql.dataTransferObjects.TriggeredAction
import com.neelkamath.omniChatBackend.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChatBackend.graphql.routing.*
import io.reactivex.rxjava3.subscribers.TestSubscriber
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.test.*

@ExtendWith(DbExtension::class)
class MutationsTest {
    @Nested
    inner class VerifyEmailAddress {
        private fun executeVerifyEmailAddress(emailAddress: String, verificationCode: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation VerifyEmailAddress(${"$"}emailAddress: String!, ${"$"}verificationCode: Int!) {
                    verifyEmailAddress(emailAddress: ${"$"}emailAddress, verificationCode: ${"$"}verificationCode) {
                        __typename
                    }
                }
                """,
                mapOf("emailAddress" to emailAddress, "verificationCode" to verificationCode),
            ).data!!["verifyEmailAddress"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The email address must get verified`() {
            val account = AccountInput(Username("u"), Password("p"), "u@example.com")
            Users.create(account)
            val code = Users.readEmailAddressVerificationCode(account.username)
            assertNull(executeVerifyEmailAddress(account.emailAddress, code))
            assertTrue(Users.hasVerifiedEmailAddress(account.emailAddress))
        }

        @Test
        fun `The email address mustn't get verified when an invalid verification code gets used`() {
            val account = AccountInput(Username("u"), Password("p"), "u@example.com")
            Users.create(account)
            val actual = executeVerifyEmailAddress(account.emailAddress, verificationCode = -1)
            assertEquals("InvalidVerificationCode", actual)
            assertFalse(Users.hasVerifiedEmailAddress(account.emailAddress))
        }

        @Test
        fun `Attempting to verify an unregistered email address must fail`(): Unit =
            assertEquals("UnregisteredEmailAddress", executeVerifyEmailAddress("u@example.com", verificationCode = -1))
    }

    @Nested
    inner class BlockUser {
        private fun executeBlockUser(blockerUserId: Int, blockedUserId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation BlockUser(${"$"}id: Int!) {
                    blockUser(id: ${"$"}id) {
                        __typename
                    }
                }
                """,
                mapOf("id" to blockedUserId),
                blockerUserId,
            ).data!!["blockUser"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The specified user must get blocked`() {
            val (blockerUserId, blockedUserId) = createVerifiedUsers(2).map { it.userId }
            assertNull(executeBlockUser(blockerUserId, blockedUserId))
            assertEquals(1, BlockedUsers.count())
        }

        @Test
        fun `Nothing must happen if the specified user gets blocked a second time`() {
            val (blockerUserId, blockedUserId) = createVerifiedUsers(2).map { it.userId }
            repeat(2) { assertNull(executeBlockUser(blockerUserId, blockedUserId)) }
            assertEquals(1, BlockedUsers.count())
        }

        @Test
        fun `Nothing must happen if the user blocks themselves`() {
            val userId = createVerifiedUsers(1).first().userId
            assertNull(executeBlockUser(blockerUserId = userId, blockedUserId = userId))
            assertEquals(0, BlockedUsers.count())
        }

        @Test
        fun `Attempting to block a non-existing user must fail`() {
            val blockerUserId = createVerifiedUsers(1).first().userId
            assertEquals("InvalidUserId", executeBlockUser(blockerUserId, blockedUserId = -1))
            assertEquals(0, BlockedUsers.count())
        }
    }

    @Nested
    inner class UnblockUser {
        private fun executeUnblockUser(blockerUserId: Int, blockedUserId: Int): Boolean = executeGraphQlViaEngine(
            """
            mutation UnblockUser(${"$"}id: Int!) {
                unblockUser(id: ${"$"}id)
            }
            """,
            mapOf("id" to blockedUserId),
            blockerUserId,
        ).data!!["unblockUser"] as Boolean

        @Test
        fun `The specified user must get unblocked`() {
            val (blockerUserId, blockedUserId) = createVerifiedUsers(2).map { it.userId }
            BlockedUsers.create(blockerUserId, blockedUserId)
            assertTrue(executeUnblockUser(blockerUserId, blockedUserId))
            assertEquals(0, BlockedUsers.count())
        }

        @Test
        fun `Attempting to unblock a user who wasn't blocked must state as such`() {
            val (blockerUserId, blockedUserId) = createVerifiedUsers(2).map { it.userId }
            assertFalse(executeUnblockUser(blockerUserId, blockedUserId))
        }

        @Test
        fun `Attempting to unblock a non-existing user must state as such`() {
            val blockerUserId = createVerifiedUsers(1).first().userId
            assertFalse(executeUnblockUser(blockerUserId, blockedUserId = -1))
        }
    }

    @Nested
    inner class CreateAccount {
        private fun executeCreateAccount(account: AccountInput): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation CreateAccount(${"$"}account: AccountInput!) {
                    createAccount(account: ${"$"}account) {
                        __typename
                    }
                }
                """,
                mapOf("account" to account),
            ).data!!["createAccount"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `An account must get created`() {
            val account = AccountInput(Username("u"), Password("p"), "u@example.com")
            assertNull(executeCreateAccount(account))
            assertEquals(1, Users.count())
        }

        @Test
        fun `Attempting to create an account with an existing username must fail`() {
            val username = Username("u")
            val existingAccount = AccountInput(username, Password("p"), "u@example.com")
            Users.create(existingAccount)
            val newAccount = AccountInput(username, Password("p"), "u@example.com")
            assertEquals("UsernameTaken", executeCreateAccount(newAccount))
            assertEquals(1, Users.count())
        }

        @Test
        fun `Attempting to create an account with an existing email address must fail`() {
            val emailAddress = "u@example.com"
            val existingAccount = AccountInput(Username("u1"), Password("p"), emailAddress)
            Users.create(existingAccount)
            val newAccount = AccountInput(Username("u2"), Password("p"), emailAddress)
            assertEquals("EmailAddressTaken", executeCreateAccount(newAccount))
            assertEquals(1, Users.count())
        }

        @Test
        fun `Attempting to create an account with a disallowed email address domain must fail`() {
            val account = AccountInput(Username("u"), Password("p"), "u@invalid.com")
            assertEquals("InvalidDomain", executeCreateAccount(account))
            assertEquals(0, Users.count())
        }
    }

    @Nested
    inner class SetOnline {
        private fun assertStatus(isOnline: Boolean) {
            val userId = createVerifiedUsers(1).first().userId
            val errors = executeGraphQlViaEngine(
                """
                mutation SetOnline(${"$"}isOnline: Boolean!) {
                    setOnline(isOnline: ${"$"}isOnline)
                }
                """,
                mapOf("isOnline" to isOnline),
                userId,
            ).errors
            assertNull(errors)
            assertEquals(isOnline, Users.isOnline(userId))
        }

        @Test
        fun `The user's status must be set to be online`(): Unit = assertStatus(isOnline = true)

        @Test
        fun `The user's status must be set to be offline`(): Unit = assertStatus(isOnline = false)
    }

    @Nested
    inner class CreateContact {
        private fun executeCreateContact(contactOwnerUserId: Int, contactUserId: Int): Boolean =
            executeGraphQlViaEngine(
                """
                mutation CreateContact(${"$"}id: Int!) {
                    createContact(id: ${"$"}id)
                }
                """,
                mapOf("id" to contactUserId),
                contactOwnerUserId,
            ).data!!["createContact"] as Boolean

        @Test
        fun `The contact must be saved`() {
            val (contactOwnerUserId, contactUserId) = createVerifiedUsers(2).map { it.userId }
            assertTrue(executeCreateContact(contactOwnerUserId, contactUserId))
            assertEquals(linkedHashSetOf(contactUserId), Contacts.readIdList(contactOwnerUserId))
        }

        @Test
        fun `A preexisting contact mustn't get saved again`() {
            val (contactOwnerUserId, contactUserId) = createVerifiedUsers(2).map { it.userId }
            assertTrue(executeCreateContact(contactOwnerUserId, contactUserId))
            assertFalse(executeCreateContact(contactOwnerUserId, contactUserId))
            assertEquals(linkedHashSetOf(contactUserId), Contacts.readIdList(contactOwnerUserId))
        }

        @Test
        fun `Attempting to save a non-existing user as a contact must fail`() {
            val contactOwnerUserId = createVerifiedUsers(1).first().userId
            assertFalse(executeCreateContact(contactOwnerUserId, contactUserId = -1))
            assertEquals(0, Contacts.count())
        }
    }

    @Nested
    inner class CreateStatus {
        private fun executeCreateStatus(userId: Int, messageId: Int, status: MessageStatus): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation CreateStatus(${"$"}messageId: Int!, ${"$"}status: MessageStatus!) {
                    createStatus(messageId: ${"$"}messageId, status: ${"$"}status) {
                        __typename
                    }
                }
                """,
                mapOf("messageId" to messageId, "status" to status),
                userId,
            ).data!!["createStatus"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `A delivery status must get created`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            val messageId = Messages.message(adminId, chatId)
            assertNull(executeCreateStatus(userId, messageId, MessageStatus.DELIVERED))
            val actual = MessageStatuses.readIdList(messageId).map(MessageStatuses::readStatus)
            assertEquals(listOf(MessageStatus.DELIVERED), actual)
        }

        @Test
        fun `Only a read status must get created when a delivery status already exists`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            val messageId = Messages.message(adminId, chatId)
            MessageStatuses.create(userId, messageId, MessageStatus.DELIVERED)
            assertNull(executeCreateStatus(userId, messageId, MessageStatus.READ))
            val actual = MessageStatuses.readIdList(messageId).map(MessageStatuses::readStatus)
            assertEquals(listOf(MessageStatus.DELIVERED, MessageStatus.READ), actual)
        }

        @Test
        fun `Creating a read status on a message sans delivered status must create a delivery status too`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            val messageId = Messages.message(adminId, chatId)
            assertNull(executeCreateStatus(userId, messageId, MessageStatus.READ))
            val actual = MessageStatuses.readIdList(messageId).map(MessageStatuses::readStatus)
            assertEquals(listOf(MessageStatus.DELIVERED, MessageStatus.READ), actual)
        }

        @Test
        fun `Attempting to create a status on a message in a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals("InvalidMessageId", executeCreateStatus(userId, messageId, MessageStatus.DELIVERED))
            assertEquals(0, MessageStatuses.count())
        }

        @Test
        fun `Attempting to create a status on the user's own message must fail`() {
            val adminId = createVerifiedUsers(2).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals("InvalidMessageId", executeCreateStatus(adminId, messageId, MessageStatus.DELIVERED))
            assertEquals(0, MessageStatuses.count())
        }
    }

    @Nested
    inner class Unstar {
        @Test
        fun `The message must get unstarred`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            Stargazers.create(adminId, messageId)
            val errors = executeGraphQlViaEngine(
                """
                mutation Unstar(${"$"}messageId: Int!) {
                    unstar(messageId: ${"$"}messageId)
                }
                """,
                mapOf("messageId" to messageId),
                adminId,
            ).errors
            assertNull(errors)
            assertEquals(0, Stargazers.count())
        }
    }

    private data class CreateGroupChatResult(val __typename: String, val chatId: Int?)

    @Nested
    inner class CreateGroupChat {
        private fun executeCreateGroupChat(userId: Int, chat: Map<String, Any>): CreateGroupChatResult {
            val data = executeGraphQlViaEngine(
                """
                mutation CreateGroupChat(${"$"}chat: GroupChatInput!) {
                    createGroupChat(chat: ${"$"}chat) {
                        __typename
                        ... on CreatedChatId {
                            chatId
                        }
                    }
                }
                """,
                mapOf("chat" to chat),
                userId,
            ).data!!["createGroupChat"] as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `The chat must get created while ignoring non-existing users`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chat = mapOf(
                "title" to "T",
                "description" to "",
                "userIdList" to setOf(adminId, -1),
                "adminIdList" to setOf(adminId),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE,
            )
            val chatId = executeCreateGroupChat(adminId, chat).chatId!!
            assertEquals(GroupChatUsers.readChatIdList(adminId).first(), chatId)
            assertEquals(linkedHashSetOf(adminId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `The creator must be implicitly added to the user ID list`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chat = mapOf(
                "title" to "T",
                "description" to "",
                "userIdList" to setOf<Int>(),
                "adminIdList" to setOf(adminId),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE,
            )
            val actual = executeCreateGroupChat(adminId, chat).__typename
            assertEquals("CreatedChatId", actual)
        }

        @Test
        fun `Attempting to make a non-existing user an admin must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chat = mapOf(
                "title" to "T",
                "description" to "",
                "userIdList" to setOf<Int>(),
                "adminIdList" to setOf(userId),
                "isBroadcast" to false,
                "publicity" to GroupChatPublicity.NOT_INVITABLE,
            )
            val actual = executeCreateGroupChat(adminId, chat).__typename
            assertEquals("InvalidAdminId", actual)
            assertEquals(0, GroupChats.count())
        }
    }

    @Nested
    inner class SetTyping {
        private fun executeSetTyping(userId: Int, chatId: Int, isTyping: Boolean): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation SetTyping(${"$"}chatId: Int!, ${"$"}isTyping: Boolean!) {
                    setTyping(chatId: ${"$"}chatId, isTyping: ${"$"}isTyping) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "isTyping" to isTyping),
                userId,
            ).data!!["setTyping"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The user's status must get updated`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertNull(executeSetTyping(adminId, chatId, isTyping = true))
            assertTrue(TypingStatuses.isTyping(chatId, adminId))
            assertNull(executeSetTyping(adminId, chatId, isTyping = false))
            assertFalse(TypingStatuses.isTyping(chatId, adminId))
        }

        @Test
        fun `Attempting to update the user's typing status in a chat they aren't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertEquals("InvalidChatId", executeSetTyping(userId, chatId, isTyping = true))
        }
    }

    @Nested
    inner class CreateTextMessage {
        private fun executeCreateTextMessage(
            userId: Int,
            chatId: Int,
            contextMessageId: Int? = null,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation CreateTextMessage(${"$"}chatId: Int!, ${"$"}text: MessageText!, ${"$"}contextMessageId: Int) {
                    createTextMessage(
                        chatId: ${"$"}chatId
                        text: ${"$"}text
                        contextMessageId: ${"$"}contextMessageId
                    ) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "text" to MessageText("t"), "contextMessageId" to contextMessageId),
                userId,
            ).data!!["createTextMessage"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId), isBroadcast = true)
            assertNull(executeCreateTextMessage(adminId, chatId))
            assertEquals("MustBeAdmin", executeCreateTextMessage(userId, chatId))
        }

        @Test
        fun `The message must get created sans context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertNull(executeCreateTextMessage(adminId, chatId))
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            assertNull(executeCreateTextMessage(adminId, chatId, contextMessageId))
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertEquals("InvalidChatId", executeCreateTextMessage(userId, chatId))
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(adminId)) }
            val contextMessageId = Messages.message(adminId, chat1Id)
            assertEquals("InvalidMessageId", executeCreateTextMessage(adminId, chat2Id, contextMessageId))
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val contextMessageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals("InvalidMessageId", executeCreateTextMessage(user1Id, chatId, contextMessageId))
        }
    }

    @Nested
    inner class ForwardMessage {
        private fun executeForwardMessage(
            userId: Int,
            chatId: Int,
            messageId: Int,
            contextMessageId: Int? = null,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation ForwardMessage(${"$"}chatId: Int!, ${"$"}messageId: Int!, ${"$"}contextMessageId: Int) {
                    forwardMessage(
                        chatId: ${"$"}chatId
                        messageId: ${"$"}messageId
                        contextMessageId: ${"$"}contextMessageId
                    ) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "messageId" to messageId, "contextMessageId" to contextMessageId),
                userId,
            ).data!!["forwardMessage"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The group chat invite must be forwarded by a user who isn't in the chat the invitation is for`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val (chat1Id, chat2Id, chat3Id) = listOf(admin1Id, admin1Id, admin2Id)
                .map { GroupChats.create(setOf(it), publicity = GroupChatPublicity.INVITABLE) }
            val messageId = Messages.message(admin1Id, chat2Id, invitedChatId = chat3Id)
            assertNull(executeForwardMessage(admin1Id, chat1Id, messageId))
        }

        @Test
        fun `Attempting to forward a group chat invite to the chat being invited to must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) =
                (1..2).map { GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val messageId = Messages.message(adminId, chat1Id, invitedChatId = chat2Id)
            assertEquals("InvalidChatId", executeForwardMessage(adminId, chat2Id, messageId))
        }

        @Test
        fun `Only admins must be allowed to forward messages to broadcast chats`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val (chat1Id, chat2Id) =
                (1..2).map { GroupChats.create(setOf(adminId), setOf(userId), isBroadcast = true) }
            val messageId = Messages.message(adminId, chat1Id)
            assertNull(executeForwardMessage(adminId, chat2Id, messageId))
            assertEquals("MustBeAdmin", executeForwardMessage(userId, chat2Id, messageId))
        }

        @Test
        fun `The message must get created sans context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(adminId)) }
            val message1Id = Messages.message(adminId, chat1Id)
            assertNull(executeForwardMessage(adminId, chat2Id, message1Id))
            val message2Id = Messages.readIdList(chat2Id).first()
            assertFalse(Messages.hasContext(message2Id))
            assertNull(Messages.readContextMessageId(message2Id))
        }

        @Test
        fun `The message must get created with a context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(adminId)) }
            val message1Id = Messages.message(adminId, chat1Id)
            val contextMessageId = Messages.message(adminId, chat2Id)
            assertNull(executeForwardMessage(adminId, chat2Id, message1Id, contextMessageId))
            val message2Id = Messages.readIdList(chat2Id).last()
            assertTrue(Messages.hasContext(message2Id))
            assertEquals(contextMessageId, Messages.readContextMessageId(message2Id))
        }

        @Test
        fun `Forwarding a message to a chat the user isn't in must fail`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val chat1Id = GroupChats.create(setOf(admin1Id, admin2Id))
            val chat2Id = GroupChats.create(setOf(admin2Id))
            val messageId = Messages.message(admin1Id, chat1Id)
            assertEquals("InvalidChatId", executeForwardMessage(admin1Id, chat2Id, messageId))
        }

        @Test
        fun `Forwarding a non-existing message must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertEquals("InvalidMessageId", executeForwardMessage(adminId, chatId, messageId = -1))
        }

        @Test
        fun `Using a non-existing context message must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(adminId)) }
            val messageId = Messages.message(adminId, chat1Id)
            assertEquals("InvalidMessageId", executeForwardMessage(adminId, chat2Id, messageId, contextMessageId = -1))
        }

        @Test
        fun `Forwarding a message the user can't see must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val privateChatId = PrivateChats.create(user1Id, user2Id)
            val groupChatId = GroupChats.create(adminIdList = setOf(user1Id), userIdList = setOf(user2Id))
            val messageId = Messages.message(user1Id, privateChatId)
            PrivateChatDeletions.create(privateChatId, user1Id)
            assertEquals("InvalidMessageId", executeForwardMessage(user1Id, groupChatId, messageId))
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val privateChatId = PrivateChats.create(user1Id, user2Id)
            val groupChatId = GroupChats.create(adminIdList = setOf(user1Id), userIdList = setOf(user2Id))
            val contextMessageId = Messages.message(user1Id, privateChatId)
            PrivateChatDeletions.create(privateChatId, user1Id)
            val messageId = Messages.message(user1Id, privateChatId)
            assertEquals("InvalidMessageId", executeForwardMessage(user1Id, groupChatId, messageId, contextMessageId))
        }

        @Test
        fun `Attempting to forward a message in the chat it's from must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals("InvalidChatId", executeForwardMessage(adminId, chatId, messageId))
        }
    }

    @Nested
    inner class SetBroadcast {
        private fun executeSetBroadcast(userId: Int, chatId: Int, isBroadcast: Boolean): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation SetBroadcast(${"$"}chatId: Int!, ${"$"}isBroadcast: Boolean!) {
                    setBroadcast(chatId: ${"$"}chatId, isBroadcast: ${"$"}isBroadcast) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "isBroadcast" to isBroadcast),
                userId,
            ).data!!["setBroadcast"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The chat must become a broadcast chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertNull(executeSetBroadcast(adminId, chatId, isBroadcast = true))
        }

        @Test
        fun `The chat must stop being a broadcast chat`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), isBroadcast = true)
            assertNull(executeSetBroadcast(adminId, chatId, isBroadcast = false))
        }

        @Test
        fun `A non-admin mustn't be allowed to update the broadcast status`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            assertEquals("MustBeAdmin", executeSetBroadcast(userId, chatId, isBroadcast = true))
        }
    }

    @Nested
    inner class Star {
        private fun executeStar(userId: Int, messageId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation Star(${"$"}messageId: Int!) {
                    star(messageId: ${"$"}messageId) {
                        __typename
                    }
                }
                """,
                mapOf("messageId" to messageId),
                userId,
            ).data!!["star"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The message must get starred`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertNull(executeStar(adminId, messageId))
            assertTrue(Stargazers.hasStar(adminId, messageId))
        }

        @Test
        fun `Attempting to star a message the user can't see must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals("InvalidMessageId", executeStar(user1Id, messageId))
            assertFalse(Stargazers.hasStar(user1Id, chatId))
        }

        @Test
        fun `Attempting to star a message from a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertEquals("InvalidMessageId", executeStar(userId, messageId))
            assertFalse(Stargazers.hasStar(userId, messageId))
        }
    }

    private data class CreatePrivateChatResult(val __typename: String?, val chatId: Int?)

    @Nested
    inner class CreatePrivateChat {
        private fun executeCreatePrivateChat(user1Id: Int, user2Id: Int): CreatePrivateChatResult {
            val data = executeGraphQlViaEngine(
                """
                mutation CreatePrivateChat(${"$"}userId: Int!) {
                    createPrivateChat(userId: ${"$"}userId) {
                        __typename
                        ... on CreatedChatId {
                            chatId
                        }
                    }
                }
                """,
                mapOf("userId" to user2Id),
                user1Id,
            ).data!!["createPrivateChat"] as Map<*, *>
            return testingObjectMapper.convertValue(data)
        }

        @Test
        fun `The chat must get created only once`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val actual = executeCreatePrivateChat(user1Id, user2Id).chatId
            val chatId = PrivateChats.readChatId(user1Id, user2Id)
            assertEquals(chatId, actual)
            assertEquals(chatId, executeCreatePrivateChat(user1Id, user2Id).chatId)
        }

        @Test
        fun `Attempting to create a chat with a non-existing user must fail`() {
            val userId = createVerifiedUsers(1).first().userId
            assertEquals("InvalidUserId", executeCreatePrivateChat(userId, user2Id = -1).__typename)
        }
    }

    @Nested
    inner class DeleteAccount {
        private fun executeDeleteAccount(userId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation DeleteAccount {
                    deleteAccount {
                        __typename
                    }
                }
                """,
                userId = userId,
            ).data!!["deleteAccount"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The account must get deleted along with its data`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            PrivateChats.create(user1Id, user2Id)
            assertNull(executeDeleteAccount(user1Id))
            assertEquals(0, PrivateChats.count())
        }

        @Test
        fun `Attempting to delete the last admin of an otherwise nonempty chat must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            GroupChats.create(setOf(adminId), setOf(userId))
            assertEquals("CannotDeleteAccount", executeDeleteAccount(adminId))
            assertEquals(1, GroupChats.count())
        }
    }

    @Nested
    inner class DeleteContact {
        private fun executeDeleteContact(contactOwnerUserId: Int, contactUserId: Int): Boolean =
            executeGraphQlViaEngine(
                """
                mutation DeleteContact(${"$"}id: Int!) {
                    deleteContact(id: ${"$"}id)
                }
                """,
                mapOf("id" to contactUserId),
                contactOwnerUserId,
            ).data!!["deleteContact"] as Boolean

        @Test
        fun `The contact must get deleted`() {
            val (contactOwnerUserId, contactUserId) = createVerifiedUsers(2).map { it.userId }
            Contacts.create(contactOwnerUserId, contactUserId)
            assertTrue(executeDeleteContact(contactOwnerUserId, contactUserId))
            assertEquals(0, Contacts.count())
        }

        @Test
        fun `Attempting to delete a non-existing user from the user's contacts must fail`() {
            val contactOwnerUserId = createVerifiedUsers(1).first().userId
            assertFalse(executeDeleteContact(contactOwnerUserId, contactUserId = -1))
        }

        @Test
        fun `Attempting to delete an existing user from the user's contacts who isn't in their contacts must fail`() {
            val (contactOwnerUserId, userId) = createVerifiedUsers(2).map { it.userId }
            assertFalse(executeDeleteContact(contactOwnerUserId, userId))
        }
    }

    @Nested
    inner class DeleteMessage {
        private fun executeDeleteMessage(userId: Int, messageId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation DeleteMessage(${"$"}id: Int!) {
                    deleteMessage(id: ${"$"}id) {
                        __typename
                    }
                }
                """,
                mapOf("id" to messageId),
                userId,
            ).data!!["deleteMessage"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The message must get deleted`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            assertNull(executeDeleteMessage(adminId, messageId))
            assertEquals(0, Messages.count())
        }

        @Test
        fun `The user mustn't be allowed to delete another user's message`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            assertEquals("InvalidMessageId", executeDeleteMessage(user2Id, messageId))
        }

        @Test
        fun `Deleting a message which is no longer visible to the user must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            assertEquals("InvalidMessageId", executeDeleteMessage(user1Id, messageId))
        }
    }

    @Nested
    inner class DeletePrivateChat {
        private fun executeDeletePrivateChat(userId: Int, chatId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation DeletePrivateChat(${"$"}chatId: Int!) {
                    deletePrivateChat(chatId: ${"$"}chatId) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId),
                userId,
            ).data!!["deletePrivateChat"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The chat must get deleted, and the user's starred messages from the chat must get deleted`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(user1Id, chatId)
            Stargazers.create(user1Id, messageId)
            assertNull(executeDeletePrivateChat(user1Id, chatId))
            assertEquals(0, Stargazers.count())
        }

        @Test
        fun `Attempting to delete a chat the user isn't in must fail`() {
            val (user1Id, user2Id, user3Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertEquals("InvalidChatId", executeDeletePrivateChat(user3Id, chatId))
            assertEquals(1, PrivateChats.count())
        }
    }

    @Nested
    inner class EmailPasswordResetCode {
        private fun executeEmailPasswordResetCode(emailAddress: String): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation EmailPasswordResetCode(${"$"}emailAddress: String!) {
                    emailPasswordResetCode(emailAddress: ${"$"}emailAddress) {
                        __typename
                    }
                }
                """,
                mapOf("emailAddress" to emailAddress),
            ).data!!["emailPasswordResetCode"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The reset code must get sent`() {
            val emailAddress = createVerifiedUsers(1).first().emailAddress
            assertNull(executeEmailPasswordResetCode(emailAddress))
        }

        @Test
        fun `Attempting to send am email to an unregistered email address must fail`(): Unit =
            assertEquals("UnregisteredEmailAddress", executeEmailPasswordResetCode("u@example.com"))
    }

    @Nested
    inner class UpdateAccount {
        private fun executeUpdateAccount(userId: Int, update: AccountUpdate): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation UpdateAccount(${"$"}update: AccountUpdate!) {
                    updateAccount(update: ${"$"}update) {
                        __typename
                    }
                }
                """,
                mapOf("update" to update),
                userId,
            ).data!!["updateAccount"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Only the non-'null' fields must get updated`() {
            val userId = createVerifiedUsers(1).first().userId
            val update = AccountUpdate(Username("u"), lastName = Name("n"))
            assertNull(executeUpdateAccount(userId, update))
            assertEquals(update.username, Users.readUsername(userId))
            assertEquals(update.lastName, Users.readLastName(userId))
        }

        @Test
        fun `None of the updates must take place if even one of the fields were invalid`() {
            val (user1, user2) = createVerifiedUsers(2)
            val update = AccountUpdate(user2.username, lastName = Name("n"))
            assertEquals("UsernameTaken", executeUpdateAccount(user1.userId, update))
            assertEquals(user1.username, Users.readUsername(user1.userId))
            assertEquals(user1.lastName, Users.readLastName(user1.userId))
        }

        @Test
        fun `Attempting to update the account to use a taken username must fail`() {
            val (user1, user2) = createVerifiedUsers(2)
            val actual = executeUpdateAccount(user1.userId, AccountUpdate(user2.username))
            assertEquals("UsernameTaken", actual)
        }

        @Test
        fun `Attempting to update the account to use the user's current username mustn't fail`() {
            val user = createVerifiedUsers(1).first()
            executeUpdateAccount(user.userId, AccountUpdate(user.username)).let(::assertNull)
        }

        @Test
        fun `Attempting to update the account to use a taken email address must fail`() {
            val (user1, user2) = createVerifiedUsers(2)
            val actual = executeUpdateAccount(user1.userId, AccountUpdate(emailAddress = user2.emailAddress))
            assertEquals("EmailAddressTaken", actual)
        }

        @Test
        fun `Attempting to update the account to use the user's current email address mustn't fail`() {
            val user = createVerifiedUsers(1).first()
            executeUpdateAccount(user.userId, AccountUpdate(emailAddress = user.emailAddress)).let(::assertNull)
        }
    }

    @Nested
    inner class DeleteProfilePic {
        @Test
        fun `The user's profile pic must get deleted`() {
            val userId = createVerifiedUsers(1).first().userId
            Users.updatePic(userId, readPic("76px×57px.jpg"))
            executeGraphQlViaEngine(
                """
                mutation DeleteProfilePic {
                    deleteProfilePic
                }
                """,
                userId = userId,
            ).errors.let(::assertNull)
            assertNull(Users.readPic(userId, PicType.THUMBNAIL))
        }
    }

    @Nested
    inner class DeleteGroupChatPic {
        private fun executeDeleteGroupChatPic(userId: Int, chatId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation DeleteGroupChatPic(${"$"}chatId: Int!) {
                    deleteGroupChatPic(chatId: ${"$"}chatId) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId),
                userId,
            ).data!!["deleteGroupChatPic"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Only the admin must be allowed to delete the pic`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            GroupChats.updatePic(chatId, readPic("76px×57px.jpg"))
            assertEquals("MustBeAdmin", executeDeleteGroupChatPic(userId, chatId))
            assertNotNull(GroupChats.readPic(chatId, PicType.THUMBNAIL))
            assertNull(executeDeleteGroupChatPic(adminId, chatId))
            assertNull(GroupChats.readPic(chatId, PicType.THUMBNAIL))
        }
    }

    @Nested
    inner class EmailEmailAddressVerification {
        private fun executeEmailEmailAddressVerification(emailAddress: String): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation EmailEmailAddressVerification(${"$"}emailAddress: String!) {
                    emailEmailAddressVerification(emailAddress: ${"$"}emailAddress) {
                        __typename
                    }
                }
                """,
                mapOf("emailAddress" to emailAddress),
            ).data!!["emailEmailAddressVerification"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The operation must succeed`() {
            val account = AccountInput(Username("u"), Password("p"), "u@example.com")
            Users.create(account)
            assertNull(executeEmailEmailAddressVerification(account.emailAddress))
        }

        @Test
        fun `Attempting to send an email to an unregistered email address must fail`(): Unit =
            assertEquals("UnregisteredEmailAddress", executeEmailEmailAddressVerification("u@example.com"))

        @Test
        fun `Attempting to send an email to a user with a verified email address must fail`() {
            val emailAddress = createVerifiedUsers(1).first().emailAddress
            assertEquals("EmailAddressVerified", executeEmailEmailAddressVerification(emailAddress))
        }
    }

    @Nested
    inner class ResetPassword {
        private fun executeResetPassword(emailAddress: String, passwordResetCode: Int, newPassword: Password): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation ResetPassword(
                    ${"$"}emailAddress: String!
                    ${"$"}passwordResetCode: Int!
                    ${"$"}newPassword: Password!
                ) {
                    resetPassword(
                        emailAddress: ${"$"}emailAddress
                        passwordResetCode: ${"$"}passwordResetCode
                        newPassword: ${"$"}newPassword
                    ) {
                        __typename
                    }
                }
                """,
                mapOf(
                    "emailAddress" to emailAddress,
                    "passwordResetCode" to passwordResetCode,
                    "newPassword" to newPassword,
                ),
            ).data!!["resetPassword"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The password must get updated`() {
            val (_, username, emailAddress) = createVerifiedUsers(1).first()
            val resetCode = Users.readPasswordResetCode(emailAddress)
            val password = Password("new")
            assertNull(executeResetPassword(emailAddress, resetCode, password))
            Users.isValidLogin(Login(username, password)).let(::assertTrue)
        }

        @Test
        fun `Attempting to reset the password using an incorrect reset code must fail`() {
            val (_, username, emailAddress, _, _, _, password) = createVerifiedUsers(1).first()
            val actual = executeResetPassword(emailAddress, passwordResetCode = -1, Password("new"))
            assertEquals("InvalidPasswordResetCode", actual)
            Users.isValidLogin(Login(username, password)).let(::assertTrue)
        }

        @Test
        fun `Attempting to reset the password of an unregistered email address must fail`() {
            val actual = executeResetPassword("u@example.com", passwordResetCode = 1, Password("p"))
            assertEquals("UnregisteredEmailAddress", actual)
        }
    }

    @Nested
    inner class UpdateGroupChatTitle {
        private fun executeUpdateGroupChatTitle(
            userId: Int,
            chatId: Int,
            title: GroupChatTitle,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation UpdateGroupChatTitle(${"$"}chatId: Int!, ${"$"}title: GroupChatTitle!) {
                    updateGroupChatTitle(chatId: ${"$"}chatId, title: ${"$"}title) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "title" to title),
                userId,
            ).data!!["updateGroupChatTitle"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Only the admin must be allowed to update the title`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            val original = GroupChats.readTitle(chatId)
            val new = GroupChatTitle("new")
            assertEquals("MustBeAdmin", executeUpdateGroupChatTitle(userId, chatId, new))
            assertEquals(original, GroupChats.readTitle(chatId))
            assertNull(executeUpdateGroupChatTitle(adminId, chatId, new))
            assertEquals(new, GroupChats.readTitle(chatId))
        }
    }

    @Nested
    inner class UpdateGroupChatDescription {
        private fun executeUpdateGroupChatDescription(
            userId: Int,
            chatId: Int,
            description: GroupChatDescription,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation UpdateGroupChatDescription(${"$"}chatId: Int!, ${"$"}description: GroupChatDescription!) {
                    updateGroupChatDescription(chatId: ${"$"}chatId, description: ${"$"}description) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "description" to description),
                userId,
            ).data!!["updateGroupChatDescription"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Only the admin must be allowed to update the description`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            val original = GroupChats.readDescription(chatId)
            val new = GroupChatDescription("new")
            assertEquals("MustBeAdmin", executeUpdateGroupChatDescription(userId, chatId, new))
            assertEquals(original, GroupChats.readDescription(chatId))
            assertNull(executeUpdateGroupChatDescription(adminId, chatId, new))
            assertEquals(new, GroupChats.readDescription(chatId))
        }
    }

    @Nested
    inner class AddGroupChatUsers {
        private fun executeAddGroupChatUsers(userId: Int, chatId: Int, userIdList: List<Int>): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation AddGroupChatUsers(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
                    addGroupChatUsers(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "userIdList" to userIdList),
                userId,
            ).data!!["addGroupChatUsers"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `A non-admin mustn't be allowed to add users`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(user1Id))
            assertEquals("MustBeAdmin", executeAddGroupChatUsers(user1Id, chatId, listOf(user2Id)))
        }

        @Test
        fun `Users must get added while ignoring non-existing users, and users who are already in the chat`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(user1Id))
            executeAddGroupChatUsers(adminId, chatId, listOf(user1Id, user2Id, -1)).let(::assertNull)
            assertEquals(linkedHashSetOf(adminId, user1Id, user2Id), GroupChatUsers.readUserIdList(chatId))
        }
    }

    @Nested
    inner class RemoveGroupChatUsers {
        private fun executeRemoveGroupChatUsers(
            userId: Int,
            chatId: Int,
            userIdList: List<Int>,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation RemoveGroupChatUsers(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
                    removeGroupChatUsers(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "userIdList" to userIdList),
                userId,
            ).data!!["removeGroupChatUsers"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `A removed user's messages and votes mustn't get deleted`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), setOf(userId))
            Messages.message(userId, chatId)
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            val pollId = Messages.message(adminId, chatId, poll)
            PollMessages.setVote(userId, pollId, poll.options[0], vote = true)
            executeRemoveGroupChatUsers(adminId, chatId, listOf(userId)).let(::assertNull)
            assertEquals(linkedHashSetOf(adminId), GroupChatUsers.readUserIdList(chatId))
            assertEquals(2, Messages.count())
            assertEquals(1, PollMessageVotes.count())
        }

        @Test
        fun `Only the removed user's stars must get deleted`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val messageId = Messages.message(adminId, chatId)
            listOf(adminId, userId).forEach { Stargazers.create(it, messageId) }
            executeRemoveGroupChatUsers(adminId, chatId, listOf(userId)).let(::assertNull)
            assertEquals(1, Stargazers.count())
        }

        @Test
        fun `A non-admin mustn't be allowed to remove users`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(user1Id, user2Id))
            val actual = executeRemoveGroupChatUsers(user1Id, chatId, listOf(user2Id))
            assertEquals("MustBeAdmin", actual)
            assertEquals(linkedHashSetOf(adminId, user1Id, user2Id), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `Non-existing users, and users who aren't in the chat will be ignored while removing users`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(user1Id))
            val userIdList = listOf(user1Id, user2Id, -1)
            assertNull(executeRemoveGroupChatUsers(adminId, chatId, userIdList))
            assertEquals(linkedHashSetOf(adminId), GroupChatUsers.readUserIdList(chatId))
        }

        @Test
        fun `Attempting to remove the last admin of an otherwise nonempty chat must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val actual = executeRemoveGroupChatUsers(adminId, chatId, listOf(adminId))
            assertEquals("CannotLeaveChat", actual)
        }

        @Test
        fun `Removing the last admin of an otherwise empty chat must succeed`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            executeRemoveGroupChatUsers(adminId, chatId, listOf(adminId)).let(::assertNull)
        }

        @Test
        fun `Removing another admin from the chat must succeed`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(admin1Id, admin2Id))
            executeRemoveGroupChatUsers(admin1Id, chatId, listOf(admin2Id)).let(::assertNull)
        }

        @Test
        fun `The user must be able to remove themselves from an otherwise nonempty chat`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(admin1Id, admin2Id))
            executeRemoveGroupChatUsers(admin1Id, chatId, listOf(admin1Id)).let(::assertNull)
        }
    }

    @Nested
    inner class LeaveGroupChat {
        private fun executeLeaveGroupChat(userId: Int, chatId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation LeaveGroupChat(${"$"}chatId: Int!) {
                    leaveGroupChat(chatId: ${"$"}chatId) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId),
                userId,
            ).data!!["leaveGroupChat"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The user must leave, and their stars must get deleted only for them`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val messageId = Messages.message(userId, chatId)
            listOf(adminId, userId).forEach { Stargazers.create(it, messageId) }
            assertNull(executeLeaveGroupChat(userId, chatId))
            assertEquals(1, Stargazers.count())
        }

        @Test
        fun `Attempting to leave a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertEquals("InvalidChatId", executeLeaveGroupChat(userId, chatId))
        }

        @Test
        fun `Leaving an otherwise empty chat must succeed`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            assertNull(executeLeaveGroupChat(adminId, chatId))
        }

        @Test
        fun `Attempting to leave an otherwise nonempty chat where the user is the last admin must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertEquals("CannotLeaveChat", executeLeaveGroupChat(adminId, chatId))
        }

        @Test
        fun `Leaving an otherwise nonempty chat in which there's another admin must succeed`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(admin1Id, admin2Id))
            assertNull(executeLeaveGroupChat(admin1Id, chatId))
        }
    }

    @Nested
    inner class MakeGroupChatAdmins {
        private fun executeMakeGroupChatAdmins(userId: Int, chatId: Int, userIdList: List<Int>): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation MakeGroupChatAdmins(${"$"}chatId: Int!, ${"$"}userIdList: [Int!]!) {
                    makeGroupChatAdmins(chatId: ${"$"}chatId, userIdList: ${"$"}userIdList) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "userIdList" to userIdList),
                userId,
            ).data!!["makeGroupChatAdmins"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Only admins must be allowed to create admins`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(user1Id, user2Id))
            val actual1 = executeMakeGroupChatAdmins(user1Id, chatId, listOf(user2Id))
            assertEquals("MustBeAdmin", actual1)
            executeMakeGroupChatAdmins(adminId, chatId, listOf(user1Id)).let(::assertNull)
        }

        @Test
        fun `Users must be made admins while ignoring non-existing users, users who aren't in the chat, and users who are already admins`() {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(user1Id))
            val userIdList = listOf(adminId, user1Id, user2Id, -1)
            assertNull(executeMakeGroupChatAdmins(adminId, chatId, userIdList))
            assertEquals(linkedHashSetOf(adminId, user1Id), GroupChatUsers.readUserIdList(chatId))
        }
    }

    @Nested
    inner class CreatePollMessage {
        private fun executeCreatePollMessage(
            userId: Int,
            chatId: Int,
            poll: Map<String, Any>,
            contextMessageId: Int? = null,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation CreatePollMessage(${"$"}chatId: Int!, ${"$"}poll: PollInput!, ${"$"}contextMessageId: Int) {
                    createPollMessage(
                        chatId: ${"$"}chatId
                        poll: ${"$"}poll
                        contextMessageId: ${"$"}contextMessageId
                    ) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "poll" to poll, "contextMessageId" to contextMessageId),
                userId,
            ).data!!["createPollMessage"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        private fun executeCreatePollMessage(
            userId: Int,
            chatId: Int,
            poll: PollInput,
            contextMessageId: Int? = null,
        ): String? = executeCreatePollMessage(
            userId,
            chatId,
            testingObjectMapper.convertValue<Map<String, Any>>(poll),
            contextMessageId,
        )

        @Test
        fun `A non-admin must be allowed to create a message in a non-broadcast chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertNull(executeCreatePollMessage(userId, chatId, poll))
        }

        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId), isBroadcast = true)
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertEquals("MustBeAdmin", executeCreatePollMessage(userId, chatId, poll))
            assertEquals(0, Messages.count())
            assertNull(executeCreatePollMessage(adminId, chatId, poll))
            assertEquals(1, Messages.count())
        }

        @Test
        fun `The message must get created sans context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertNull(executeCreatePollMessage(adminId, chatId, poll))
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertNull(executeCreatePollMessage(adminId, chatId, poll, contextMessageId))
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertEquals("InvalidChatId", executeCreatePollMessage(userId, chatId, poll))
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(adminId)) }
            val contextMessageId = Messages.message(adminId, chat1Id)
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertEquals("InvalidMessageId", executeCreatePollMessage(adminId, chat2Id, poll, contextMessageId))
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val contextMessageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            assertEquals("InvalidMessageId", executeCreatePollMessage(user1Id, chatId, poll, contextMessageId))
        }

        private fun assertInvalidPoll(hasDuplicateOption: Boolean) {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val options = if (hasDuplicateOption) listOf("option", "option") else listOf("option")
            val poll = mapOf("question" to "Q", "options" to options)
            assertEquals("InvalidPoll", executeCreatePollMessage(adminId, chatId, poll))
        }

        @Test
        fun `Attempting to create a poll with fewer than two options must fail`(): Unit =
            assertInvalidPoll(hasDuplicateOption = false)

        @Test
        fun `Attempting to create a poll with duplicate options must fail`(): Unit =
            assertInvalidPoll(hasDuplicateOption = true)
    }

    @Nested
    inner class SetPollVote {
        private fun executeSetPollVote(userId: Int, messageId: Int, option: MessageText, vote: Boolean): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation SetPollVote(${"$"}messageId: Int!, ${"$"}option: MessageText!, ${"$"}vote: Boolean!) {
                    setPollVote(messageId: ${"$"}messageId, option: ${"$"}option, vote: ${"$"}vote) {
                        __typename
                    }
                }
                """,
                mapOf("messageId" to messageId, "option" to option, "vote" to vote),
                userId,
            ).data!!["setPollVote"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `A non-admin must be able to vote in a broadcast chat`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId), isBroadcast = true)
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            assertNull(executeSetPollVote(userId, messageId, poll.options[0], vote = true))
            assertEquals(1, PollMessageVotes.count())
        }

        @Test
        fun `A user must be allowed to vote for multiple options but not multiple times for the same option`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            repeat(2) {
                assertNull(executeSetPollVote(adminId, messageId, poll.options[0], vote = true))
                assertEquals(1, PollMessageVotes.count())
            }
            assertNull(executeSetPollVote(adminId, messageId, poll.options[1], vote = true))
            assertEquals(2, PollMessageVotes.count())
        }

        @Test
        fun `A user must be allowed to vote on their own poll`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            assertNull(executeSetPollVote(adminId, messageId, poll.options[0], vote = true))
            assertEquals(1, PollMessageVotes.count())
        }

        @Test
        fun `Deleting votes, including votes the user never made, must succeed`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val option = poll.options[0]
            val verify = {
                assertNull(executeSetPollVote(adminId, messageId, option, vote = false))
                assertEquals(0, PollMessageVotes.count())
            }
            verify()
            PollMessages.setVote(adminId, messageId, option, vote = true)
            verify()
        }

        @Test
        fun `Attempting to vote on a non-existing poll must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val actual = executeSetPollVote(adminId, messageId = -1, MessageText("option"), vote = true)
            assertEquals("InvalidMessageId", actual)
        }

        @Test
        fun `Attempting to vote on a non-existing option must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val poll = PollInput(MessageText("Question"), listOf(MessageText("Option 1"), MessageText("Option 2")))
            val messageId = Messages.message(adminId, chatId, poll)
            val actual = executeSetPollVote(adminId, messageId, MessageText("Non-existing option"), vote = true)
            assertEquals("NonexistingOption", actual)
        }
    }

    @Nested
    inner class JoinGroupChat {
        private fun executeJoinGroupChat(userId: Int, inviteCode: UUID): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation JoinGroupChat(${"$"}inviteCode: Uuid!) {
                    joinGroupChat(inviteCode: ${"$"}inviteCode) {
                        __typename
                    }
                }
                """,
                mapOf("inviteCode" to inviteCode),
                userId,
            ).data!!["joinGroupChat"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The user must join the specified chat at most once`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val code = GroupChats.readInviteCode(chatId)!!
            repeat(2) {
                assertNull(executeJoinGroupChat(userId, code))
                assertEquals(linkedHashSetOf(adminId, userId), GroupChatUsers.readUserIdList(chatId))
            }
        }

        @Test
        fun `Using an invite code for a non-invitable chat must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val code = GroupChats.readInviteCode(chatId)!!
            GroupChats.setPublicity(chatId, isInvitable = false)
            assertEquals("InvalidInviteCode", executeJoinGroupChat(userId, code))
            assertEquals(linkedHashSetOf(adminId), GroupChatUsers.readUserIdList(chatId))
        }
    }

    @Nested
    inner class JoinPublicChat {
        private fun executeJoinPublicChat(userId: Int, chatId: Int): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation JoinPublicChat(${"$"}chatId: Int!) {
                    joinPublicChat(chatId: ${"$"}chatId) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId),
                userId,
            ).data!!["joinPublicChat"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The chat must be joined at most once`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            repeat(2) {
                assertNull(executeJoinPublicChat(userId, chatId))
                assertEquals(linkedHashSetOf(adminId, userId), GroupChatUsers.readUserIdList(chatId))
            }
        }

        @Test
        fun `Attempting to join a non-public chat must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            assertEquals("InvalidChatId", executeJoinPublicChat(userId, chatId))
        }
    }

    @Nested
    inner class CreateGroupChatInviteMessage {
        private fun executeCreateGroupChatInviteMessage(
            userId: Int,
            chatId: Int,
            invitedChatId: Int,
            contextMessageId: Int? = null,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation CreateGroupChatInviteMessage(
                    ${"$"}chatId: Int!
                    ${"$"}invitedChatId: Int!
                    ${"$"}contextMessageId: Int
                ) {
                    createGroupChatInviteMessage(
                        chatId: ${"$"}chatId
                        invitedChatId: ${"$"}invitedChatId
                        contextMessageId: ${"$"}contextMessageId
                    ) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "invitedChatId" to invitedChatId, "contextMessageId" to contextMessageId),
                userId,
            ).data!!["createGroupChatInviteMessage"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `Attempting to create an invite message for the chat in the chat itself must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            assertEquals("InvalidChatId", executeCreateGroupChatInviteMessage(adminId, chatId, invitedChatId = chatId))
        }

        @Test
        fun `Given a user who is in chat 1 but not in chat 2, when they attempt to create an invite message for chat 2 in chat 1, then it mustn't succeed`() {
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.userId }
            val (chatId, invitedChatId) = listOf(admin1Id, admin2Id)
                .map { GroupChats.create(setOf(it), publicity = GroupChatPublicity.INVITABLE) }
            assertEquals("InvalidChatId", executeCreateGroupChatInviteMessage(admin1Id, chatId, invitedChatId))
        }

        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val (chatId, invitedChatId) = (1..2).map {
                GroupChats.create(
                    listOf(adminId),
                    listOf(userId),
                    isBroadcast = true,
                    publicity = GroupChatPublicity.INVITABLE,
                )
            }
            assertNull(executeCreateGroupChatInviteMessage(adminId, chatId, invitedChatId))
            assertEquals("MustBeAdmin", executeCreateGroupChatInviteMessage(userId, chatId, invitedChatId))
        }

        @Test
        fun `The message must get created sans context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chatId, invitedChatId) =
                (1..2).map { GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            assertNull(executeCreateGroupChatInviteMessage(adminId, chatId, invitedChatId))
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chatId, invitedChatId) =
                (1..2).map { GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val contextMessageId = Messages.message(adminId, chatId)
            assertNull(executeCreateGroupChatInviteMessage(adminId, chatId, invitedChatId, contextMessageId))
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val (chatId, invitedChatId) =
                (1..2).map { GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            assertEquals("InvalidChatId", executeCreateGroupChatInviteMessage(userId, chatId, invitedChatId))
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) =
                (1..2).map { GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE) }
            val contextMessageId = Messages.message(adminId, chat1Id)
            val actual = executeCreateGroupChatInviteMessage(
                adminId,
                chatId = chat2Id,
                invitedChatId = chat1Id,
                contextMessageId,
            )
            assertEquals("InvalidMessageId", actual)
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val invitedChatId =
                GroupChats.create(adminIdList = listOf(user1Id), publicity = GroupChatPublicity.INVITABLE)
            val contextMessageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val actual = executeCreateGroupChatInviteMessage(
                user1Id,
                chatId, invitedChatId,
                contextMessageId,
            )
            assertEquals("InvalidMessageId", actual)
        }

        @Test
        fun `Attempting to create an invite message for a chat which isn't invitable must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.INVITABLE)
            val invitedChatId = GroupChats.create(setOf(adminId))
            assertEquals("InvalidInvitedChat", executeCreateGroupChatInviteMessage(adminId, chatId, invitedChatId))
        }
    }

    @Nested
    inner class SetPublicity {
        private fun executeSetPublicity(
            userId: Int,
            chatId: Int,
            isInvitable: Boolean,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation SetPublicity(${"$"}chatId: Int!, ${"$"}isInvitable: Boolean!) {
                    setPublicity(chatId: ${"$"}chatId, isInvitable: ${"$"}isInvitable) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "chatId" to chatId, "isInvitable" to isInvitable),
                userId,
            ).data!!["setPublicity"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        @Test
        fun `The publicity must get changed`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            listOf(true, false).forEach { isInvitable ->
                assertNull(executeSetPublicity(adminId, chatId, isInvitable))
                assertEquals(isInvitable, GroupChats.isInvitable(chatId))
            }
        }

        @Test
        fun `Attempting to set the publicity for a chat which isn't a group chat must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            assertEquals("InvalidChatId", executeSetPublicity(user1Id, chatId, isInvitable = false))
        }

        @Test
        fun `Attempting to set the publicity of a public chat must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId), publicity = GroupChatPublicity.PUBLIC)
            assertEquals("InvalidChatId", executeSetPublicity(adminId, chatId, isInvitable = false))
        }

        @Test
        fun `Only admins must be allowed to update the publicity`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            assertEquals("MustBeAdmin", executeSetPublicity(userId, chatId, isInvitable = false))
            assertNull(executeSetPublicity(adminId, chatId, isInvitable = false))
        }
    }

    @Nested
    inner class CreateActionMessage {
        private fun executeCreateActionMessage(
            userId: Int,
            chatId: Int,
            message: Map<String, Any>,
            contextMessageId: Int? = null,
        ): String? {
            val data = executeGraphQlViaEngine(
                """
                mutation CreateActionMessage(
                    ${"$"}chatId: Int!
                    ${"$"}message: ActionMessageInput!
                    ${"$"}contextMessageId: Int
                ) {
                    createActionMessage(
                        chatId: ${"$"}chatId
                        message: ${"$"}message
                        contextMessageId: ${"$"}contextMessageId
                    ) {
                        __typename
                    }
                }
                """,
                mapOf("chatId" to chatId, "message" to message, "contextMessageId" to contextMessageId),
                userId,
            ).data!!["createActionMessage"] as Map<*, *>?
            return data?.get("__typename") as String?
        }

        private fun executeCreateActionMessage(
            userId: Int,
            chatId: Int,
            message: ActionMessageInput,
            contextMessageId: Int? = null,
        ): String? = executeCreateActionMessage(
            userId,
            chatId,
            testingObjectMapper.convertValue<Map<String, Any>>(message),
            contextMessageId,
        )

        @Test
        fun `Only admins must be allowed to message in broadcast chats`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId), isBroadcast = true)
            val message = ActionMessageInput(MessageText("Title"), listOf(MessageText("Action 1")))
            assertNull(executeCreateActionMessage(adminId, chatId, message))
            assertEquals("MustBeAdmin", executeCreateActionMessage(userId, chatId, message))
        }

        @Test
        fun `The message must get created sans context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val message = ActionMessageInput(MessageText("Title"), listOf(MessageText("Action 1")))
            assertNull(executeCreateActionMessage(adminId, chatId, message))
            val messageId = Messages.readIdList(chatId).first()
            assertFalse(Messages.hasContext(messageId))
            assertNull(Messages.readContextMessageId(messageId))
        }

        @Test
        fun `The message must get created with a context`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val contextMessageId = Messages.message(adminId, chatId)
            val message = ActionMessageInput(MessageText("Title"), listOf(MessageText("Action 1")))
            assertNull(executeCreateActionMessage(adminId, chatId, message, contextMessageId))
            val messageId = Messages.readIdList(chatId).last()
            assertTrue(Messages.hasContext(messageId))
            assertEquals(contextMessageId, Messages.readContextMessageId(messageId))
        }

        @Test
        fun `Attempting to create a message in a chat the user isn't in must fail`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            val message = ActionMessageInput(MessageText("Title"), listOf(MessageText("Action 1")))
            assertEquals("InvalidChatId", executeCreateActionMessage(userId, chatId, message))
            assertEquals(0, Messages.count())
        }

        @Test
        fun `Referencing a context message from another chat must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val (chat1Id, chat2Id) = (1..2).map { GroupChats.create(setOf(adminId)) }
            val contextMessageId = Messages.message(adminId, chat1Id)
            val message = ActionMessageInput(MessageText("Title"), listOf(MessageText("Action 1")))
            assertEquals("InvalidMessageId", executeCreateActionMessage(adminId, chat2Id, message, contextMessageId))
            assertEquals(1, Messages.count())
        }

        @Test
        fun `Using a message the user can't see as a context must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val contextMessageId = Messages.message(user1Id, chatId)
            PrivateChatDeletions.create(chatId, user1Id)
            val message = ActionMessageInput(MessageText("Title"), listOf(MessageText("Action 1")))
            assertEquals("InvalidMessageId", executeCreateActionMessage(user1Id, chatId, message, contextMessageId))
        }

        private fun assertActions(hasDuplicateAction: Boolean) {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val actions = if (hasDuplicateAction) listOf(MessageText("a"), MessageText("a")) else listOf()
            val message = mapOf("text" to MessageText("t"), "actions" to actions)
            assertEquals("InvalidAction", executeCreateActionMessage(adminId, chatId, message))
        }

        @Test
        fun `Duplicate actions must be disallowed`(): Unit = assertActions(hasDuplicateAction = true)

        @Test
        fun `At least one action must exist`(): Unit = assertActions(hasDuplicateAction = false)
    }

    @Nested
    inner class TriggerAction {
        private fun executeTriggerAction(userId: Int, messageId: Int, action: MessageText): Boolean =
            executeGraphQlViaEngine(
                """
                mutation TriggerAction(${"$"}messageId: Int!, ${"$"}action: MessageText!) {
                    triggerAction(messageId: ${"$"}messageId, action: ${"$"}action)
                }
                """,
                mapOf("messageId" to messageId, "action" to action),
                userId,
            ).data!!["triggerAction"] as Boolean

        @Test
        fun `The action must get triggered by a non-admin in a broadcast chat`(): Unit = runBlocking {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId), listOf(userId))
            val action = MessageText("a")
            val message = ActionMessageInput(MessageText("text"), listOf(action))
            val messageId = Messages.message(adminId, chatId, message)
            awaitBrokering()
            val subscriber = messagesNotifier.subscribe(UserId(adminId)).flowable.subscribeWith(TestSubscriber())
            assertTrue(executeTriggerAction(userId, messageId, action))
            awaitBrokering()
            val values = subscriber.values().map { it as TriggeredAction }
            assertEquals(listOf(messageId), values.map { it.getMessageId() })
            assertEquals(listOf(action), values.map { it.getAction() })
            assertEquals(listOf(userId), values.map { it.getTriggeredBy().id })
        }

        @Test
        fun `Attempting to trigger a non-existing action must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            val chatId = GroupChats.create(setOf(adminId))
            val message = ActionMessageInput(MessageText("text"), listOf(MessageText("action")))
            val messageId = Messages.message(adminId, chatId, message)
            assertFalse(executeTriggerAction(adminId, messageId, MessageText("non-existing action")))
        }

        @Test
        fun `Attempting to trigger an action on a message the user can't see must fail`() {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.userId }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val action = MessageText("a")
            val message = ActionMessageInput(MessageText("text"), listOf(action))
            val messageId = Messages.message(user1Id, chatId, message)
            PrivateChatDeletions.create(chatId, user1Id)
            assertFalse(executeTriggerAction(user1Id, messageId, action))
        }

        @Test
        fun `Attempting to trigger an action on a non-existing action message must fail`() {
            val adminId = createVerifiedUsers(1).first().userId
            executeTriggerAction(adminId, messageId = -1, MessageText("action")).let(::assertFalse)
        }

        @Test
        fun `The user mustn't be allowed to trigger an action in a chat they aren't in`() {
            val (adminId, userId) = createVerifiedUsers(2).map { it.userId }
            val chatId = GroupChats.create(setOf(adminId))
            val message = ActionMessageInput(MessageText("text"), listOf(MessageText("action")))
            val messageId = Messages.message(adminId, chatId, message)
            assertFalse(executeTriggerAction(userId, messageId, message.actions[0]))
        }
    }
}
