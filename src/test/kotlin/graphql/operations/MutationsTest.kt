package com.neelkamath.omniChat.graphql.operations

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.db.count
import com.neelkamath.omniChat.db.tables.*
import com.neelkamath.omniChat.graphql.*
import com.neelkamath.omniChat.graphql.engine.executeGraphQlViaEngine
import com.neelkamath.omniChat.routing.executeGraphQlViaHttp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

const val SET_BROADCAST_STATUS_QUERY = """
    mutation SetBroadcastStatus(${"$"}chatId: Int!, ${"$"}isBroadcast: Boolean!) {
        setBroadcastStatus(chatId: ${"$"}chatId, isBroadcast: ${"$"}isBroadcast)
    }
"""

private fun operateSetBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): GraphQlResponse =
    executeGraphQlViaEngine(SET_BROADCAST_STATUS_QUERY, mapOf("chatId" to chatId, "isBroadcast" to isBroadcast), userId)

fun setBroadcastStatus(userId: Int, chatId: Int, isBroadcast: Boolean): Placeholder {
    val data = operateSetBroadcastStatus(userId, chatId, isBroadcast).data!!["setBroadcastStatus"] as String
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
}

fun errUpdateGroupChatDescription(userId: Int, chatId: Int, description: GroupChatDescription): String =
    operateUpdateGroupChatDescription(userId, chatId, description).errors!![0].message

const val UPDATE_GROUP_CHAT_TITLE_QUERY = """
    mutation UpdateGroupChatTitle(${"$"}chatId: Int!, ${"$"}title: GroupChatTitle!) {
        updateGroupChatTitle(chatId: ${"$"}chatId, title: ${"$"}title)
    }
"""

private fun operateUpdateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): GraphQlResponse =
    executeGraphQlViaEngine(UPDATE_GROUP_CHAT_TITLE_QUERY, mapOf("chatId" to chatId, "title" to title.value), userId)

fun updateGroupChatTitle(userId: Int, chatId: Int, title: GroupChatTitle): Placeholder {
    val data = operateUpdateGroupChatTitle(userId, chatId, title).data!!["updateGroupChatTitle"] as String
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
}

fun errDeleteGroupChatPic(userId: Int, chatId: Int): String =
    operateDeleteGroupChatPic(userId, chatId).errors!![0].message

const val DELETE_PROFILE_PIC_QUERY = """
    mutation DeleteProfilePic {
        deleteProfilePic
    }
"""

private fun operateDeleteProfilePic(userId: Int): GraphQlResponse =
    executeGraphQlViaEngine(DELETE_PROFILE_PIC_QUERY, userId = userId)

fun deleteProfilePic(userId: Int): Placeholder {
    val data = operateDeleteProfilePic(userId).data!!["deleteProfilePic"] as String
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    mutation CreateMessage(${"$"}chatId: Int!, ${"$"}text: TextMessage!, ${"$"}contextMessageId: Int) {
        createMessage(chatId: ${"$"}chatId, text: ${"$"}text, contextMessageId: ${"$"}contextMessageId)
    }
"""

private fun operateCreateMessage(
    userId: Int,
    chatId: Int,
    text: TextMessage,
    contextMessageId: Int? = null
): GraphQlResponse = executeGraphQlViaEngine(
    CREATE_MESSAGE_QUERY,
    mapOf("chatId" to chatId, "text" to text, "contextMessageId" to contextMessageId),
    userId
)

fun createMessage(userId: Int, chatId: Int, text: TextMessage, contextMessageId: Int? = null): Placeholder {
    val data = operateCreateMessage(userId, chatId, text, contextMessageId).data!!["createMessage"] as String
    return objectMapper.convertValue(data)
}

fun errCreateMessage(userId: Int, chatId: Int, text: TextMessage, contextMessageId: Int? = null): String =
    operateCreateMessage(userId, chatId, text, contextMessageId).errors!![0].message

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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
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
    return objectMapper.convertValue(data)
}

fun errUpdateAccount(userId: Int, update: AccountUpdate): String =
    operateUpdateAccount(userId, update).errors!![0].message

class MutationsTest : FunSpec({
    context("setBroadcastStatus(DataFetchingEnvironment)") {
        test("Only an admin should be allowed to set the broadcast status") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                SET_BROADCAST_STATUS_QUERY,
                mapOf("chatId" to chatId, "isBroadcast" to true),
                user.accessToken
            ).status() shouldBe HttpStatusCode.Unauthorized
        }

        test("The user shouldn't be allowed to update a chat they aren't in") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            errSetBroadcastStatus(userId, chatId, isBroadcast = true) shouldBe InvalidChatIdException.message
        }

        test("The broadcast status should be updated") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val isBroadcast = true
            setBroadcastStatus(adminId, chatId, isBroadcast)
            GroupChats.readChat(adminId, chatId).isBroadcast shouldBe isBroadcast
        }
    }

    context("makeGroupChatAdmins(DataFetchingEnvironment)") {
        test("Making a user who isn't in the chat an admin should fail") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            errMakeGroupChatAdmins(adminId, chatId, listOf(userId)) shouldBe InvalidUserIdException.message
        }

        test("The users should be made admins") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId), listOf(userId))
            makeGroupChatAdmins(adminId, chatId, listOf(userId))
            GroupChatUsers.readAdminIdList(chatId) shouldBe listOf(adminId, userId)
        }
    }

    context("readGroupChatUpdate(DataFetchingEnvironment)") {
        test("An exception should be thrown if a non-admin updates the chat") {
            val (admin, user) = createVerifiedUsers(2).map { it }
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(
                UPDATE_GROUP_CHAT_DESCRIPTION_QUERY,
                mapOf("chatId" to chatId, "description" to "description"),
                user.accessToken
            ).shouldHaveUnauthorizedStatus()
        }

        test("An exception should be thrown when the user updates the a chat they aren't in") {
            val (adminId, userId) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(adminId))
            errUpdateGroupChatDescription(userId, chatId, GroupChatDescription("")) shouldBe
                    InvalidChatIdException.message
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
    }

    context("updateGroupChatDescription(DataFetchingEnvironment)") {
        test("The admin should update the description") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val description = GroupChatDescription("New description.")
            updateGroupChatDescription(adminId, chatId, description)
            GroupChats.readChat(adminId, chatId).description shouldBe description
        }
    }

    context("updateGroupChatTitle(DataFetchingEnvironment)") {
        test("The admin should update the title") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val title = GroupChatTitle("New Title")
            updateGroupChatTitle(adminId, chatId, title)
            GroupChats.readChat(adminId, chatId).title shouldBe title
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
            GroupChats.updatePic(chatId, Pic.build("31KB.png"))
            deleteGroupChatPic(adminId, chatId)
            GroupChats.readPic(chatId).shouldBeNull()
        }

        test("An exception should be thrown when a non-admin updates the pic") {
            val (admin, user) = createVerifiedUsers(2)
            val chatId = GroupChats.create(listOf(admin.info.id), listOf(user.info.id))
            executeGraphQlViaHttp(DELETE_GROUP_CHAT_PIC_QUERY, mapOf("chatId" to chatId), user.accessToken)
                .shouldHaveUnauthorizedStatus()
        }

        test("Updating the pic of a nonexistent chat should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            errDeleteGroupChatPic(adminId, chatId = 1) shouldBe InvalidChatIdException.message
        }
    }

    context("deleteProfilePic(DataFetchingEnvironment)") {
        test("The user's profile pic should be deleted") {
            val userId = createVerifiedUsers(1)[0].info.id
            Users.updatePic(userId, Pic.build("31KB.png"))
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
                "isBroadcast" to false
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

        test("A group chat shouldn't be created when supplied with an invalid user ID") {
            val userId = createVerifiedUsers(1)[0].info.id
            val invalidUserId = -1
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(userId, invalidUserId),
                adminIdList = listOf(userId),
                isBroadcast = false
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
                "isBroadcast" to false
            )
            executeGraphQlViaEngine(
                CREATE_GROUP_CHAT_QUERY,
                mapOf("chat" to chat),
                user1Id
            ).errors!![0].message shouldBe InvalidAdminIdException.message
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
            val (admin1Id, admin2Id) = createVerifiedUsers(2).map { it.info.id }
            val chatId = GroupChats.create(listOf(admin1Id))
            GroupChats.create(listOf(admin2Id))
            errCreateMessage(admin2Id, chatId, TextMessage("t")) shouldBe InvalidChatIdException.message
        }

        test("The message should be created sans context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            createMessage(adminId, chatId, TextMessage("t"))
            Messages.readGroupChat(adminId, chatId).map { it.node.context } shouldBe
                    listOf(MessageContext(hasContext = false, id = null))
        }

        test("The message should be created with a context") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            val messageId = Messages.message(adminId, chatId)
            createMessage(adminId, chatId, TextMessage("t"), contextMessageId = messageId)
            Messages.readGroupChat(adminId, chatId)[1].node.context shouldBe
                    MessageContext(hasContext = true, id = messageId)
        }

        test("Using a nonexistent message context should fail") {
            val adminId = createVerifiedUsers(1)[0].info.id
            val chatId = GroupChats.create(listOf(adminId))
            errCreateMessage(adminId, chatId, TextMessage("t"), contextMessageId = 1) shouldBe
                    InvalidMessageIdException.message
        }

        test("A non-admin shouldn't be allowed to message in a broadcast chat") {
            val (admin, user) = createVerifiedUsers(2)
            val chat = GroupChatInput(
                GroupChatTitle("T"),
                GroupChatDescription(""),
                userIdList = listOf(admin.info.id, user.info.id),
                adminIdList = listOf(admin.info.id),
                isBroadcast = true
            )
            val chatId = GroupChats.create(chat)
            executeGraphQlViaHttp(
                CREATE_MESSAGE_QUERY,
                mapOf("chatId" to chatId, "text" to "Hi"),
                user.accessToken
            ).status() shouldBe HttpStatusCode.Unauthorized
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
            Messages.readGroupChat(adminId, chatId).shouldBeEmpty()
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
