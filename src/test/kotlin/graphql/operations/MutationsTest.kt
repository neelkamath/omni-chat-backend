package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.routing.executeGraphQlViaHttp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

const val DELETE_PROFILE_PIC_QUERY = """
    mutation DeleteProfilePic {
        deleteProfilePic
    }
"""

fun operateDeleteProfilePic(userId: String): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PROFILE_PIC_QUERY, userId = userId)

fun deleteProfilePic(userId: String): Placeholder {
    val data = operateDeleteProfilePic(userId).data!!["deleteProfilePic"] as String
    return objectMapper.convertValue(data)
}

const val CREATE_ACCOUNTS_QUERY = """
    mutation CreateAccount(${"$"}account: NewAccount!) {
        createAccount(account: ${"$"}account)
    }
"""

fun operateCreateAccount(account: NewAccount): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_ACCOUNTS_QUERY, mapOf("account" to account))

fun createAccount(account: NewAccount): Placeholder {
    val data = operateCreateAccount(account).data!!["createAccount"] as String
    return objectMapper.convertValue(data)
}

fun errCreateAccount(account: NewAccount): String = operateCreateAccount(account).errors!![0].message

const val CREATE_CONTACTS_QUERY = """
    mutation CreateContacts(${"$"}userIdList: [String!]!) {
        createContacts(userIdList: ${"$"}userIdList)
    }
"""

fun operateCreateContacts(userId: String, userIdList: List<String>): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_CONTACTS_QUERY, mapOf("userIdList" to userIdList), userId)

fun createContacts(userId: String, userIdList: List<String>): Placeholder {
    val data = operateCreateContacts(userId, userIdList).data!!["createContacts"] as String
    return objectMapper.convertValue(data)
}

fun errCreateContacts(userId: String, userIdList: List<String>): String =
    operateCreateContacts(userId, userIdList).errors!![0].message

const val CREATE_GROUP_CHAT_QUERY = """
    mutation CreateGroupChat(${"$"}chat: NewGroupChat!) {
        createGroupChat(chat: ${"$"}chat)
    }
"""

fun operateCreateGroupChat(userId: String, chat: NewGroupChat): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_GROUP_CHAT_QUERY, mapOf("chat" to chat), userId)

fun createGroupChat(userId: String, chat: NewGroupChat): Int =
    operateCreateGroupChat(userId, chat).data!!["createGroupChat"] as Int

fun errCreateGroupChat(userId: String, chat: NewGroupChat): String =
    operateCreateGroupChat(userId, chat).errors!![0].message

const val CREATE_MESSAGE_QUERY = """
    mutation CreateMessage(${"$"}chatId: Int!, ${"$"}text: TextMessage!) {
        createMessage(chatId: ${"$"}chatId, text: ${"$"}text)
    }
"""

fun operateCreateMessage(userId: String, chatId: Int, message: TextMessage): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_MESSAGE_QUERY, mapOf("chatId" to chatId, "text" to message), userId)

fun createMessage(userId: String, chatId: Int, message: TextMessage): Placeholder {
    val data = operateCreateMessage(userId, chatId, message).data!!["createMessage"] as String
    return objectMapper.convertValue(data)
}

fun errCreateMessage(userId: String, chatId: Int, message: TextMessage): String =
    operateCreateMessage(userId, chatId, message).errors!![0].message

const val CREATE_PRIVATE_CHAT_QUERY = """
    mutation CreatePrivateChat(${"$"}userId: ID!) {
        createPrivateChat(userId: ${"$"}userId)
    }
"""

fun operateCreatePrivateChat(userId: String, otherUserId: String): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_PRIVATE_CHAT_QUERY, mapOf("userId" to otherUserId), userId)

fun createPrivateChat(userId: String, otherUserId: String): Int =
    operateCreatePrivateChat(userId, otherUserId).data!!["createPrivateChat"] as Int

fun errCreatePrivateChat(userId: String, otherUserId: String): String =
    operateCreatePrivateChat(userId, otherUserId).errors!![0].message

const val CREATE_STATUS_QUERY = """
    mutation CreateStatus(${"$"}messageId: Int!, ${"$"}status: MessageStatus!) {
        createStatus(messageId: ${"$"}messageId, status: ${"$"}status)
    }
"""

fun operateCreateStatus(userId: String, messageId: Int, status: MessageStatus): GraphQlResponse =
    executeGraphQlViaEngine(CREATE_STATUS_QUERY, mapOf("messageId" to messageId, "status" to status), userId)

fun createStatus(userId: String, messageId: Int, status: MessageStatus): Placeholder {
    val data = operateCreateStatus(userId, messageId, status).data!!["createStatus"] as String
    return objectMapper.convertValue(data)
}

fun errCreateStatus(userId: String, messageId: Int, status: MessageStatus): String =
    operateCreateStatus(userId, messageId, status).errors!![0].message

const val DELETE_ACCOUNT_QUERY = """
    mutation DeleteAccount {
        deleteAccount
    }
"""

fun operateDeleteAccount(userId: String): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_ACCOUNT_QUERY, userId = userId)

fun deleteAccount(userId: String): Placeholder {
    val data = operateDeleteAccount(userId).data!!["deleteAccount"] as String
    return objectMapper.convertValue(data)
}

fun errDeleteAccount(userId: String): String = operateDeleteAccount(userId).errors!![0].message

const val DELETE_CONTACTS_QUERY = """
    mutation DeleteContacts(${"$"}userIdList: [String!]!) {
        deleteContacts(userIdList: ${"$"}userIdList)
    }
"""

fun operateDeleteContacts(userId: String, userIdList: List<String>): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_CONTACTS_QUERY, mapOf("userIdList" to userIdList), userId)

fun deleteContacts(userId: String, userIdList: List<String>): Placeholder {
    val data = operateDeleteContacts(userId, userIdList).data!!["deleteContacts"] as String
    return objectMapper.convertValue(data)
}

const val DELETE_MESSAGE_QUERY = """
    mutation DeleteMessage(${"$"}id: Int!) {
        deleteMessage(id: ${"$"}id)
    }
"""

fun operateDeleteMessage(userId: String, messageId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_MESSAGE_QUERY, mapOf("id" to messageId), userId)

fun deleteMessage(userId: String, messageId: Int): Placeholder {
    val data = operateDeleteMessage(userId, messageId).data!!["deleteMessage"] as String
    return objectMapper.convertValue(data)
}

fun errDeleteMessage(userId: String, messageId: Int): String =
    operateDeleteMessage(userId, messageId).errors!![0].message

const val DELETE_PRIVATE_CHAT_QUERY = """
    mutation DeletePrivateChat(${"$"}chatId: Int!) {
        deletePrivateChat(chatId: ${"$"}chatId)
    }
"""

fun operateDeletePrivateChat(userId: String, chatId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PRIVATE_CHAT_QUERY, mapOf("chatId" to chatId), userId)

fun deletePrivateChat(userId: String, chatId: Int): Placeholder {
    val data = operateDeletePrivateChat(userId, chatId).data!!["deletePrivateChat"] as String
    return objectMapper.convertValue(data)
}

fun errDeletePrivateChat(userId: String, chatId: Int): String =
    operateDeletePrivateChat(userId, chatId).errors!![0].message

const val LEAVE_GROUP_CHAT_QUERY = """
    mutation LeaveGroupChat(${"$"}chatId: Int!) {
        leaveGroupChat(chatId: ${"$"}chatId)
    }
"""

fun operateLeaveGroupChat(userId: String, chatId: Int): GraphQlResponse =
    executeGraphQlViaEngine(LEAVE_GROUP_CHAT_QUERY, mapOf("chatId" to chatId), userId)

fun leaveGroupChat(userId: String, chatId: Int): Placeholder {
    val data = operateLeaveGroupChat(userId, chatId).data!!["leaveGroupChat"] as String
    return objectMapper.convertValue(data)
}

fun errLeaveGroupChat(userId: String, chatId: Int): String =
    operateLeaveGroupChat(userId, chatId).errors!![0].message

const val RESET_PASSWORD_QUERY = """
    mutation ResetPassword(${"$"}emailAddress: String!) {
        resetPassword(emailAddress: ${"$"}emailAddress)
    }
"""

fun operateResetPassword(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(RESET_PASSWORD_QUERY, mapOf("emailAddress" to emailAddress))

fun resetPassword(emailAddress: String): Placeholder {
    val data = operateResetPassword(emailAddress).data!!["resetPassword"] as String
    return objectMapper.convertValue(data)
}

fun errResetPassword(emailAddress: String): String = operateResetPassword(emailAddress).errors!![0].message

const val SEND_EMAIL_ADDRESS_VERIFICATION_QUERY = """
    mutation SendEmailAddressVerification(${"$"}emailAddress: String!) {
        sendEmailAddressVerification(emailAddress: ${"$"}emailAddress)
    }
"""

fun operateSendEmailAddressVerification(emailAddress: String): GraphQlResponse =
    executeGraphQlViaEngine(SEND_EMAIL_ADDRESS_VERIFICATION_QUERY, mapOf("emailAddress" to emailAddress))

fun sendEmailAddressVerification(emailAddress: String): Placeholder {
    val data = operateSendEmailAddressVerification(emailAddress).data!!["sendEmailAddressVerification"] as String
    return objectMapper.convertValue(data)
}

fun errSendEmailVerification(emailAddress: String): String =
    operateSendEmailAddressVerification(emailAddress).errors!![0].message

const val UPDATE_ACCOUNT_QUERY = """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
        updateAccount(update: ${"$"}update)
    }
"""

fun operateUpdateAccount(userId: String, update: AccountUpdate): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_ACCOUNT_QUERY, mapOf("update" to update), userId)

fun updateAccount(userId: String, update: AccountUpdate): Placeholder {
    val data = operateUpdateAccount(userId, update).data!!["updateAccount"] as String
    return objectMapper.convertValue(data)
}

fun errUpdateAccount(userId: String, update: AccountUpdate): String =
    operateUpdateAccount(userId, update).errors!![0].message

const val UPDATE_GROUP_CHAT_QUERY = """
    mutation UpdateGroupChat(${"$"}update: GroupChatUpdate!) {
        updateGroupChat(update: ${"$"}update)
    }
"""

fun operateUpdateGroupChat(userId: String, update: GroupChatUpdate): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_GROUP_CHAT_QUERY, mapOf("update" to update), userId)

fun updateGroupChat(userId: String, update: GroupChatUpdate): Placeholder {
    val data = operateUpdateGroupChat(userId, update).data!!["updateGroupChat"] as String
    return objectMapper.convertValue(data)
}

fun errUpdateGroupChat(userId: String, update: GroupChatUpdate): String =
    operateUpdateGroupChat(userId, update).errors!![0].message

class MutationsTest : FunSpec({
    context("deleteProfilePic(DataFetchingEnvironment)") {
        test("The user's profile pic should be deleted") {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.setProfilePic(userId)
            deleteProfilePic(userId)
            Users.readProfilePic(userId).shouldBeNull()
        }
    }

    context("createAccount(DataFetchingEnvironment)") {
        test("Creating an account should save it to the auth system, and the DB") {
            val account = NewAccount(Username("username"), Password("password"), "username@example.com")
            createAccount(account)
            with(readUserByUsername(account.username)) {
                username shouldBe account.username
                emailAddress shouldBe account.emailAddress
            }
            Users.count() shouldBe 1
        }

        test("An account with a taken username shouldn't be created") {
            val account = NewAccount(Username("username"), Password("password"), "username@example.com")
            createAccount(account)
            errCreateAccount(account) shouldBe UsernameTakenException.message
        }

        test("An account with a taken email shouldn't be created") {
            val address = "username@example.com"
            val account = NewAccount(Username("username1"), Password("password"), address)
            createAccount(account)
            val duplicateAccount = NewAccount(Username("username2"), Password("password"), address)
            errCreateAccount(duplicateAccount) shouldBe EmailAddressTakenException.message
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
            val contacts = listOf(userId, "invalid user ID")
            errCreateContacts(ownerId, contacts) shouldBe InvalidContactException.message
            Contacts.readIdList(ownerId).shouldBeEmpty()
        }
    }

    context("createGroupChat(DataFetchingEnvironment)") {
        test("A group chat should be created, ignoring the user's own ID, and keeping whitespace intact") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val chat = NewGroupChat(
                GroupChatTitle(" Title  "),
                GroupChatDescription("  Description "),
                listOf(adminId, user1Id, user2Id)
            )
            val chatId = createGroupChat(adminId, chat)
            GroupChats.readUserChats(adminId) shouldBe listOf(
                GroupChat(
                    chatId,
                    adminId,
                    GroupChatUsers.readUsers(chatId),
                    chat.title,
                    chat.description,
                    Messages.readGroupChatConnection(chatId)
                )
            )
        }

        test("A group chat shouldn't be created when supplied with an invalid user ID") {
            val userId = createVerifiedUsers(1)[0].info.id
            errCreateGroupChat(userId, buildNewGroupChat("invalid user ID")) shouldBe InvalidUserIdException.message
        }
    }

    context("createMessage(DataFetchingEnvironment)") {
        test("The user should be able to create a message in a private chat they just deleted") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            PrivateChatDeletions.create(chatId, user1Id)
            createMessage(user1Id, chatId, TextMessage("t"))
        }

        test("Messaging in a chat the user isn't in should throw an exception") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(user1Id, buildNewGroupChat())
            GroupChats.create(user1Id, buildNewGroupChat())
            errCreateMessage(user2Id, chatId, TextMessage("t")) shouldBe InvalidChatIdException.message
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
            errCreatePrivateChat(userId, "a nonexistent user ID") shouldBe InvalidUserIdException.message
        }

        test("A chat shouldn't be created with the user themselves") {
            val userId = createVerifiedUsers(1)[0].info.id
            errCreatePrivateChat(userId, userId) shouldBe InvalidUserIdException.message
        }
    }

    context("createStatus(DataFetchingEnvironment") {
        /** A private chat between two users where [user2Id] sent the [messageId]. */
        data class UtilizedPrivateChat(val messageId: Int, val user1Id: String, val user2Id: String)

        fun createUtilizedPrivateChat(): UtilizedPrivateChat {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user2Id, TextMessage("t"))
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
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
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
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
            PrivateChatDeletions.create(chatId, user2Id)
            errCreateStatus(user2Id, messageId, MessageStatus.DELIVERED) shouldBe InvalidMessageIdException.message
        }
    }

    context("deleteAccount(DataFetchingEnvironment)") {
        test("An account should be deleted from the auth service") {
            val userId = createVerifiedUsers(1)[0].info.id
            deleteAccount(userId)
            userIdExists(userId).shouldBeFalse()
        }

        test("An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            GroupChats.create(adminId, buildNewGroupChat(userId))
            errDeleteAccount(adminId) shouldBe CannotDeleteAccountException.message
        }
    }

    context("deleteContacts(DataFetchingEnvironment)") {
        test("Contacts should be deleted, ignoring invalid ones") {
            val (ownerId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val userIdList = listOf(user1Id, user2Id)
            Contacts.create(ownerId, userIdList.toSet())
            deleteContacts(ownerId, userIdList + "invalid user id")
            Contacts.readIdList(ownerId).shouldBeEmpty()
        }
    }

    context("deleteMessage(DataFetchingEnvironment)") {
        test("The user's message should be deleted") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(adminId, buildNewGroupChat())
            val messageId = Messages.message(chatId, adminId, TextMessage("t"))
            deleteMessage(adminId, messageId)
            Messages.readGroupChat(chatId).shouldBeEmpty()
        }

        test("Deleting a nonexistent message should return an error") {
            val userId = createVerifiedUsers(1)[0].info.id
            errDeleteMessage(userId, messageId = 0) shouldBe InvalidMessageIdException.message
        }

        test("Deleting a message from a chat the user isn't in should throw an exception") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(user2Id, buildNewGroupChat())
            val messageId = Messages.message(chatId, user2Id, TextMessage("t"))
            errDeleteMessage(user1Id, messageId) shouldBe InvalidMessageIdException.message
        }

        test("Deleting another user's message should return an error") {
            val (user1Id, user2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = PrivateChats.create(user1Id, user2Id)
            val messageId = Messages.message(chatId, user2Id, TextMessage("t"))
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
            val messageId = Messages.message(chatId, user1Id, TextMessage("t"))
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

    context("leaveGroupChat(DataFetchingEnvironment)") {
        test("A non-admin should leave the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            leaveGroupChat(userId, chatId)
            GroupChatUsers.readUserIdList(chatId) shouldBe listOf(adminId)
        }

        test("The admin should leave the chat if they're the only user") {
            val userId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(userId, buildNewGroupChat())
            leaveGroupChat(userId, chatId)
        }

        test("The admin shouldn't be allowed to leave if there are other users in the chat") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat(userId))
            errLeaveGroupChat(adminId, chatId) shouldBe AdminCannotLeaveException.message
        }

        test("Leaving a group chat the user is not in should throw an exception") {
            val userId = createVerifiedUsers(1)[0].info.id
            errLeaveGroupChat(userId, chatId = 1) shouldBe InvalidChatIdException.message
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
            val account = NewAccount(Username("username"), Password("password"), address)
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

    context("updateGroupChat(DataFetchingEnvironment)") {
        test("Only the supplied fields should be updated") {
            val (adminId, user1Id, user2Id) = createVerifiedUsers(3).map { it.info.id }
            val initialUserIdList = listOf(user1Id)
            val chat = buildNewGroupChat(initialUserIdList)
            val chatId = GroupChats.create(adminId, chat)
            val update = GroupChatUpdate(
                chatId,
                GroupChatTitle("New Title"),
                newUserIdList = listOf(user2Id),
                removedUserIdList = listOf(user1Id)
            )
            updateGroupChat(adminId, update)
            with(GroupChats.readUserChats(adminId)[0]) {
                adminId shouldBe adminId
                users.edges.map { it.node.id } shouldBe
                        initialUserIdList + adminId + update.newUserIdList!! - update.removedUserIdList!!
                title shouldBe update.title
                description shouldBe chat.description
            }
        }

        test("The chat's new admin should be set") {
            val (firstAdminId, secondAdminId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat(
                GroupChatTitle("T"),
                GroupChatDescription("description"),
                userIdList = listOf(secondAdminId)
            )
            val chatId = GroupChats.create(firstAdminId, chat)
            val update = GroupChatUpdate(chatId, newAdminId = secondAdminId)
            updateGroupChat(firstAdminId, update)
            GroupChats.readChat(chatId).adminId shouldBe secondAdminId
        }

        test("Updating a nonexistent chat should throw an exception") {
            val userId = createVerifiedUsers(1)[0].info.id
            errUpdateGroupChat(userId, GroupChatUpdate(chatId = 1)) shouldBe InvalidChatIdException.message
        }

        test("Updating a chat the user isn't the admin of should return an authorization error") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chat = NewGroupChat(GroupChatTitle("T"), GroupChatDescription("description"), listOf(userId))
            val chatId = GroupChats.create(adminId, chat)
            val variables = mapOf("update" to GroupChatUpdate(chatId))
            executeGraphQlViaHttp(UPDATE_GROUP_CHAT_QUERY, variables, userId).shouldHaveUnauthorizedStatus()
        }

        test("Transferring admin status to a user not in the chat should throw an exception") {
            val (adminId, notInvitedUserId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat())
            val update = GroupChatUpdate(chatId, newAdminId = notInvitedUserId)
            errUpdateGroupChat(adminId, update) shouldBe InvalidNewAdminIdException.message
        }

        test("Adding a nonexistent user should fail") {
            val userId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(userId, buildNewGroupChat())
            val update = GroupChatUpdate(chatId, newUserIdList = listOf("invalid user ID"))
            errUpdateGroupChat(userId, update) shouldBe InvalidUserIdException.message
        }

        test("Adding and removing the same user at the same time should fail") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(adminId, buildNewGroupChat())
            val update = mapOf(
                "update" to mapOf(
                    "chatId" to chatId,
                    "newUserIdList" to listOf(userId),
                    "removedUserIdList" to listOf(userId)
                )
            )
            executeGraphQlViaEngine(UPDATE_GROUP_CHAT_QUERY, variables = update, userId = adminId)
                .errors!![0]
                .message shouldBe InvalidGroupChatUsersException.message
        }
    }
})