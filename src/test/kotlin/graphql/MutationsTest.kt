package com.neelkamath.omniChat.test.graphql

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.*
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.db.count
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.shouldBe

fun operateDeleteAccount(jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation DeleteAccount {
      deleteAccount
    }
    """,
    jwt = jwt
)

fun deleteAccount(jwt: String): Boolean = operateDeleteAccount(jwt).data!!["deleteAccount"] as Boolean

fun operateUpdateAccount(update: AccountUpdate, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation UpdateAccount(${"$"}update: AccountUpdate!) {
      updateAccount(update: ${"$"}update)
    }
    """,
    variables = mapOf("update" to update),
    jwt = jwt
)

fun updateAccount(update: AccountUpdate, jwt: String): Boolean =
    operateUpdateAccount(update, jwt).data!!["updateAccount"] as Boolean

fun operateCreateAccount(account: NewAccount): GraphQlResponse = operateGraphQl(
    """
    mutation CreateAccount(${"$"}account: NewAccount!) {
      createAccount(account: ${"$"}account)
    }
    """,
    variables = mapOf("account" to account)
)

fun createAccount(account: NewAccount): Boolean = operateCreateAccount(account).data!!["createAccount"] as Boolean

fun operateVerifyEmail(email: String): GraphQlResponse = operateGraphQl(
    """
    mutation VerifyEmail {
      verifyEmail(email: "$email")
    }
    """
)

fun verifyEmail(email: String): Boolean = operateVerifyEmail(email).data!!["verifyEmail"] as Boolean

fun operateResetPassword(email: String): GraphQlResponse = operateGraphQl(
    """
    mutation ResetPassword {
      resetPassword(email: "$email")
    }
    """
)

fun resetPassword(email: String): Boolean = operateResetPassword(email).data!!["resetPassword"] as Boolean

fun operateLeaveGroupChat(jwt: String, chatId: Int, newAdminId: String? = null): GraphQlResponse = operateGraphQl(
    """
    mutation LeaveGroupChat(${"$"}chatId: Int!, ${"$"}newAdminId: String) {
      leaveGroupChat(chatId: ${"$"}chatId, newAdminId: ${"$"}newAdminId)
    }
    """,
    variables = mapOf("chatId" to chatId, "newAdminId" to newAdminId),
    jwt = jwt
)

fun leaveGroupChat(jwt: String, chatId: Int, newAdminId: String? = null): Boolean =
    operateLeaveGroupChat(jwt, chatId, newAdminId).data!!["leaveGroupChat"] as Boolean

fun operateUpdateGroupChat(update: GroupChatUpdate, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation UpdateGroupChat(${"$"}update: GroupChatUpdate!) {
      updateGroupChat(update: ${"$"}update)
    }
    """,
    variables = mapOf("update" to update),
    jwt = jwt
)

fun updateGroupChat(update: GroupChatUpdate, jwt: String): Boolean =
    operateUpdateGroupChat(update, jwt).data!!["updateGroupChat"] as Boolean

fun operateCreateGroupChat(chat: NewGroupChat, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation CreateGroupChat(${"$"}chat: NewGroupChat!) {
      createGroupChat(chat: ${"$"}chat)
    }
    """,
    variables = mapOf("chat" to chat),
    jwt = jwt
)

fun createGroupChat(chat: NewGroupChat, jwt: String): Int =
    operateCreateGroupChat(chat, jwt).data!!["createGroupChat"] as Int

fun operateDeletePrivateChat(chatId: Int, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation DeletePrivateChat {
      deletePrivateChat(chatId: $chatId)
    }
    """,
    jwt = jwt
)

fun deletePrivateChat(chatId: Int, jwt: String): Boolean =
    operateDeletePrivateChat(chatId, jwt).data!!["deletePrivateChat"] as Boolean

fun operateCreatePrivateChat(userId: String, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation CreatePrivateChat {
      createPrivateChat(userId: "$userId")
    }
    """,
    jwt = jwt
)

fun createPrivateChat(userId: String, jwt: String): Int =
    operateCreatePrivateChat(userId, jwt).data!!["createPrivateChat"] as Int

fun operateMessage(message: String, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation Message {
      message(message: "$message")
    }
    """,
    jwt = jwt
)

fun message(message: String, jwt: String): Boolean = operateMessage(message, jwt).data!!["message"] as Boolean

fun operateDeleteContacts(userIdList: List<String>, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation DeleteContacts(${"$"}userIdList: [String!]!) {
      deleteContacts(userIdList: ${"$"}userIdList)
    }
    """,
    variables = mapOf("userIdList" to userIdList),
    jwt = jwt
)

fun deleteContacts(userIdList: List<String>, jwt: String): Boolean =
    operateDeleteContacts(userIdList, jwt).data!!["deleteContacts"] as Boolean

fun operateCreateContacts(userIdList: List<String>, jwt: String): GraphQlResponse = operateGraphQl(
    """
    mutation CreateContacts(${"$"}userIdList: [String!]!) {
      createContacts(userIdList: ${"$"}userIdList)
    }
    """,
    variables = mapOf("userIdList" to userIdList),
    jwt = jwt
)

fun createContacts(userIdList: List<String>, jwt: String): Boolean =
    operateCreateContacts(userIdList, jwt).data!!["createContacts"] as Boolean

class DeleteAccountTest : StringSpec({
    listener(AppListener())

    "An account should be able to be deleted if the user is the admin of an empty group chat" {
        val login = createVerifiedUsers(1)[0].login
        val jwt = requestJwt(login).jwt
        createGroupChat(NewGroupChat("Title", userIdList = setOf()), jwt)
        deleteAccount(jwt).shouldBeTrue()
    }

    "An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat" {
        val (admin, user) = createVerifiedUsers(2)
        val jwt = requestJwt(admin.login).jwt
        createGroupChat(NewGroupChat("Title", userIdList = setOf(user.info.id)), jwt)
        deleteAccount(jwt).shouldBeFalse()
    }

    "An account should be deleted from the auth service" {
        val user = createVerifiedUsers(1)[0]
        deleteAccount(requestJwt(user.login).jwt)
        Auth.userIdExists(user.info.id).shouldBeFalse()
    }

    "The user's contacts and contacts of the user should be deleted when their account is deleted" {
        val (user1, user2) = createVerifiedUsers(2)
        val user1Jwt = requestJwt(user1.login).jwt
        val user2Jwt = requestJwt(user2.login).jwt
        createContacts(listOf(user2.info.id), user1Jwt)
        createContacts(listOf(user1.info.id), user2Jwt)
        deleteAccount(user1Jwt)
        Contacts.read(user1.info.id).shouldBeEmpty()
        Contacts.read(user2.info.id).shouldBeEmpty()
    }

    "A group chat should be deleted if its only member's account is deleted" {
        val (admin, user) = createVerifiedUsers(2)
        val adminJwt = requestJwt(admin.login).jwt
        val chatId = createGroupChat(NewGroupChat("Title", userIdList = setOf(user.info.id)), adminJwt)
        leaveGroupChat(requestJwt(user.login).jwt, chatId)
        deleteAccount(adminJwt)
        GroupChats.count().shouldBeZero()
    }

    "The user whose account is deleted should be removed from group chats" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, requestJwt(admin.login).jwt)
        deleteAccount(requestJwt(user.login).jwt)
        GroupChats.read(chatId).userIdList shouldBe setOf(admin.info.id)
    }

    "Private chat clears should be empty for chats with a user who deleted their account" {
        val (creator, invitee) = createVerifiedUsers(2)
        val jwt = requestJwt(creator.login).jwt
        val chatId = createPrivateChat(invitee.info.id, jwt)
        deletePrivateChat(chatId, jwt)
        deleteAccount(jwt)
        PrivateChatClears.count().shouldBeZero()
    }

    "Private chats with a user who deleted their account should be deleted" {
        val (creator, invitee) = createVerifiedUsers(2)
        val jwt = requestJwt(creator.login).jwt
        createPrivateChat(invitee.info.id, jwt)
        deleteAccount(jwt)
        PrivateChats.read(invitee.info.id).shouldBeEmpty()
    }
})

class UpdateAccountTest : StringSpec({
    listener(AppListener())

    fun testAccountInfo(account: AccountInfo, updatedAccount: AccountUpdate) {
        Auth.usernameExists(account.username).shouldBeFalse()
        with(Auth.findUserByUsername(updatedAccount.username!!)) {
            username shouldBe updatedAccount.username
            email shouldBe updatedAccount.email
            isEmailVerified.shouldBeFalse()
            firstName shouldBe account.firstName
            lastName shouldBe updatedAccount.lastName
        }
    }

    "Only the specified fields should be updated" {
        val user = createVerifiedUsers(1)[0]
        val update = AccountUpdate(username = "john_roger", email = "john.roger@example.com", lastName = "Roger")
        updateAccount(update, requestJwt(user.login).jwt)
        testAccountInfo(user.info, update)
    }

    "The password should be updated" {
        val login = createVerifiedUsers(1)[0].login
        val jwt = requestJwt(login).jwt
        val newPassword = "new password"
        updateAccount(AccountUpdate(password = newPassword), jwt)
        requestJwt(login.copy(password = newPassword)) // Successfully requesting a JWT tests the password update.
    }

    "Updating a username to one already taken shouldn't allow the account to be updated" {
        val (user1, user2) = createVerifiedUsers(2)
        val response = operateUpdateAccount(AccountUpdate(username = user2.info.username), requestJwt(user1.login).jwt)
        response.errors!![0].message shouldBe UsernameTakenException().message
    }

    "Updating an email to one already taken shouldn't allow the account to be updated" {
        val (user1, user2) = createVerifiedUsers(2)
        val response = operateUpdateAccount(AccountUpdate(email = user2.info.email), requestJwt(user1.login).jwt)
        response.errors!![0].message shouldBe EmailTakenException().message
    }
})

class CreateAccountTest : StringSpec({
    listener(AppListener())

    "An account should be created" {
        val account = NewAccount("username", "password", "username@example.com")
        createAccount(account)
        with(Auth.findUserByUsername(account.username)) {
            username shouldBe account.username
            email shouldBe account.email
        }
    }

    "An account with a taken username shouldn't be created" {
        val account = NewAccount("username", "password", "username@example.com")
        createAccount(account)
        operateCreateAccount(account).errors!![0].message shouldBe UsernameTakenException().message
    }

    "An account with a taken email shouldn't be created" {
        val email = "username@example.com"
        createAccount(NewAccount("username1", "password", email))
        val response = operateCreateAccount(NewAccount("username2", "password", email))
        response.errors!![0].message shouldBe EmailTakenException().message
    }
})

class VerifyEmailTest : StringSpec({
    listener(AppListener())

    "A verification email should be sent" {
        val email = "username@example.com"
        createAccount(NewAccount("username", "password", email))
        verifyEmail(email).shouldBeTrue()
    }

    "Sending a verification email to an unregistered address should throw an error" {
        operateVerifyEmail("username@example.com").errors!![0].message shouldBe UnregisteredEmailException().message
    }
})

class ResetPasswordTest : StringSpec({
    listener(AppListener())

    "A password reset request should be sent" { resetPassword(createVerifiedUsers(1)[0].info.email).shouldBeTrue() }

    "Requesting a password reset for an unregistered address should throw an error" {
        operateResetPassword("username@example.com").errors!![0].message shouldBe UnregisteredEmailException().message
    }
})

class LeaveGroupChatTest : StringSpec({
    listener(AppListener())

    "A non-admin should leave the chat" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, requestJwt(admin.login).jwt)
        leaveGroupChat(requestJwt(user.login).jwt, chatId).shouldBeTrue()
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(admin.info.id)
    }

    "The admin should leave the chat after specifying the new admin if there are users left in the chat" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, requestJwt(admin.login).jwt)
        leaveGroupChat(requestJwt(admin.login).jwt, chatId, newAdminId = user.info.id).shouldBeTrue()
        GroupChatUsers.readUserIdList(chatId) shouldBe setOf(user.info.id)
    }

    "The admin should leave the chat without specifying a new admin if they are the last user" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(admin.info.id, user.info.id))
        val adminJwt = requestJwt(admin.login).jwt
        val chatId = createGroupChat(chat, adminJwt)
        leaveGroupChat(requestJwt(user.login).jwt, chatId)
        leaveGroupChat(adminJwt, chatId).shouldBeTrue()
    }

    fun testBadUserId(supplyingId: Boolean) {
        val (admin, user) = createVerifiedUsers(2)
        val jwt = requestJwt(admin.login).jwt
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, jwt)
        val newAdminId = if (supplyingId) "invalid new admin ID" else null
        val exception = if (supplyingId) InvalidNewAdminIdException() else MissingNewAdminIdException()
        operateLeaveGroupChat(jwt, chatId, newAdminId).errors!![0].message shouldBe exception.message
    }

    "The admin shouldn't be allowed to leave without specifying a new admin if there are users left" {
        testBadUserId(supplyingId = false)
    }

    "The admin shouldn't be allowed to leave the chat if the new admin's user ID is invalid" {
        testBadUserId(supplyingId = true)
    }

    "Leaving a group chat the user is not in should throw an error" {
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        operateLeaveGroupChat(jwt, chatId = 1).errors!![0].message shouldBe InvalidChatIdException().message
    }
})

class UpdateGroupChatTest : StringSpec({
    listener(AppListener())

    "Only the supplied fields should be updated" {
        val (creator, user1, user2) = createVerifiedUsers(3)
        val jwt = requestJwt(creator.login).jwt
        val initialUserIdList = setOf(user1.info.id)
        val chat = NewGroupChat("Title", "description", initialUserIdList)
        val chatId = createGroupChat(chat, jwt)
        val update = GroupChatUpdate(
            chatId,
            "New Title",
            newUserIdList = setOf(user2.info.id),
            removedUserIdList = setOf(user1.info.id)
        )
        updateGroupChat(update, jwt).shouldBeTrue()
        val userIdList = initialUserIdList + creator.info.id + update.newUserIdList - update.removedUserIdList
        val groupChat = GroupChat(chatId, creator.info.id, userIdList, update.title!!, chat.description)
        GroupChats.read(creator.info.id) shouldBe listOf(groupChat)
    }

    "The chat's admin should be switched" {
        val (firstAdmin, secondAdmin) = createVerifiedUsers(2)
        val jwt = requestJwt(firstAdmin.login).jwt
        val chat = NewGroupChat("Title", userIdList = setOf(secondAdmin.info.id))
        val chatId = createGroupChat(chat, jwt)
        updateGroupChat(GroupChatUpdate(chatId, newAdminId = secondAdmin.info.id), jwt).shouldBeTrue()
        GroupChats.read(chatId).adminId shouldBe secondAdmin.info.id
    }

    "Transferring admin status to a user not in the chat should throw an error" {
        val (admin, invitedUser, notInvitedUser) = createVerifiedUsers(3)
        val jwt = requestJwt(admin.login).jwt
        val chat = NewGroupChat("Title", userIdList = setOf(invitedUser.info.id))
        val chatId = createGroupChat(chat, jwt)
        val update = GroupChatUpdate(chatId, newAdminId = notInvitedUser.info.id)
        operateUpdateGroupChat(update, jwt).errors!![0].message shouldBe InvalidNewAdminIdException().message
    }

    "Updating a nonexistent chat should throw an error" {
        val login = createVerifiedUsers(1)[0].login
        val response = operateUpdateGroupChat(GroupChatUpdate(chatId = 1), requestJwt(login).jwt)
        response.errors!![0].message shouldBe InvalidChatIdException().message
    }

    "Updating a chat the user isn't the admin of should throw an error" {
        val (admin, user) = createVerifiedUsers(2)
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, requestJwt(admin.login).jwt)
        val response = operateUpdateGroupChat(GroupChatUpdate(chatId), requestJwt(user.login).jwt)
        response.errors!![0].message shouldBe UnauthorizedException().message
    }
})

class CreateGroupChatTest : StringSpec({
    listener(AppListener())

    "A group chat should be created, ignoring the user's own user ID" {
        val (admin, user1, user2) = createVerifiedUsers(3)
        val chat = NewGroupChat(
            title = "\uD83D\uDCDA Book Club", // Test that emoji works.
            description = "Books discussion",
            userIdList = setOf(admin.info.id, user1.info.id, user2.info.id)
        )
        val chatId = createGroupChat(chat, requestJwt(admin.login).jwt)
        val userIdList = chat.userIdList + admin.info.id
        val groupChat = GroupChat(chatId, admin.info.id, userIdList, chat.title, chat.description)
        GroupChats.read(admin.info.id) shouldBe listOf(groupChat)
    }

    "A group chat should not be created when supplied with an invalid user ID" {
        val chat = NewGroupChat("Title", userIdList = setOf("invalid user ID"))
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        operateCreateGroupChat(chat, jwt).errors!![0].message shouldBe InvalidUserIdException().message
    }

    "A group chat should not be created if an empty title is supplied" {
        val chat = NewGroupChat(title = "", userIdList = setOf())
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        operateCreateGroupChat(chat, jwt).errors!![0].message shouldBe InvalidTitleLengthException().message
    }

    "A group chat should not be created if the title is too long" {
        val title = CharArray(GroupChats.maxTitleLength + 1) { 'a' }.joinToString("")
        val chat = NewGroupChat(title = title, userIdList = setOf())
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        operateCreateGroupChat(chat, jwt).errors!![0].message shouldBe InvalidTitleLengthException().message
    }

    "A group chat should not be created if the description has an invalid length" {
        val description = CharArray(GroupChats.maxDescriptionLength + 1) { 'a' }.joinToString("")
        val chat = NewGroupChat("Title", description, userIdList = setOf())
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        operateCreateGroupChat(chat, jwt).errors!![0].message shouldBe InvalidDescriptionLengthException().message
    }
})

class DeletePrivateChatTest : StringSpec({
    listener(AppListener())

    "A chat should be deleted" {
        val (creator, invitee) = createVerifiedUsers(2)
        val jwt = requestJwt(creator.login).jwt
        val chatId = createPrivateChat(invitee.info.id, jwt)
        deletePrivateChat(chatId, jwt).shouldBeTrue()
        PrivateChatClears.hasCleared(isCreator = true, chatId = chatId).shouldBeTrue()
    }

    "Deleting an invalid chat ID should throw an error" {
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        operateDeletePrivateChat(chatId = 1, jwt = jwt).errors!![0].message shouldBe InvalidChatIdException().message
    }
})

class CreatePrivateChatTest : StringSpec({
    listener(AppListener())

    "A chat should be created" {
        val (creator, invitee) = createVerifiedUsers(2)
        val jwt = requestJwt(creator.login).jwt
        val chatId = createPrivateChat(invitee.info.id, jwt)
        val metadata = PrivateChatMetadata(chatId, creator.info.id, invitee.info.id)
        PrivateChats.read(creator.info.id) shouldBe listOf(metadata)
    }

    "An existing chat shouldn't be recreated" {
        val (creator, invitee) = createVerifiedUsers(2)
        val jwt = requestJwt(creator.login).jwt
        createPrivateChat(invitee.info.id, jwt)
        operateCreatePrivateChat(invitee.info.id, jwt).errors!![0].message shouldBe ChatExistsException().message
    }

    "A chat shouldn't be created with a nonexistent user" {
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        val response = operateCreatePrivateChat("a nonexistent user ID", jwt)
        response.errors!![0].message shouldBe InvalidUserIdException().message
    }

    "A chat shouldn't be created with the user themselves" {
        val user = createVerifiedUsers(1)[0]
        val response = operateCreatePrivateChat(user.info.id, requestJwt(user.login).jwt)
        response.errors!![0].message shouldBe InvalidUserIdException().message
    }
})

class DeleteContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be deleted, ignoring invalid ones" {
        val (owner, user1, user2) = createVerifiedUsers(3)
        val userIdList = listOf(user1.info.id, user2.info.id)
        val jwt = requestJwt(owner.login).jwt
        createContacts(userIdList, jwt)
        deleteContacts(userIdList + "invalid user id", jwt).shouldBeTrue()
        Contacts.read(owner.info.id).shouldBeEmpty()
    }

    "Deleting a user should delete it from everyone's contacts" {
        val (owner, user1, user2) = createVerifiedUsers(3)
        val uploadedContacts = listOf(user1.info.id, user2.info.id)
        val jwt = requestJwt(owner.login).jwt
        createContacts(uploadedContacts, jwt)
        deleteAccount(requestJwt(user1.login).jwt)
        Contacts.read(owner.info.id) shouldBe setOf(user2.info.id)
    }
})

class CreateContactsTest : StringSpec({
    listener(AppListener())

    "Saving previously saved contacts should be ignored" {
        val (owner, user1, user2) = createVerifiedUsers(3)
        val contacts = listOf(user1.info.id, user2.info.id)
        val jwt = requestJwt(owner.login).jwt
        repeat(2) { createContacts(contacts, jwt) }
        Contacts.read(owner.info.id) shouldContainExactly contacts
    }

    "Trying to save the user's own contact should be ignored" {
        val (owner, user) = createVerifiedUsers(2)
        createContacts(listOf(owner.info.id, user.info.id), requestJwt(owner.login).jwt).shouldBeTrue()
        Contacts.read(owner.info.id) shouldBe setOf(user.info.id)
    }

    "If one of the contacts to be saved is incorrect, then none of them should be saved" {
        val (owner, user) = createVerifiedUsers(2)
        val contacts = listOf(user.info.id, "invalid user ID")
        val response = operateCreateContacts(contacts, requestJwt(owner.login).jwt)
        response.errors!![0].message shouldBe InvalidContactException().message
        Contacts.read(owner.info.id).shouldBeEmpty()
    }
})