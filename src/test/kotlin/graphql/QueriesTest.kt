package com.neelkamath.omniChat.test.graphql

import com.fasterxml.jackson.module.kotlin.convertValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.IncorrectPasswordException
import com.neelkamath.omniChat.graphql.NonexistentUserException
import com.neelkamath.omniChat.graphql.UnverifiedEmailException
import com.neelkamath.omniChat.test.AppListener
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

fun operateCanDeleteAccount(jwt: String): GraphQlResponse = operateGraphQl(
    """
    query CanDeleteAccount {
      canDeleteAccount
    }
    """,
    jwt = jwt
)

fun canDeleteAccount(jwt: String): Boolean = operateCanDeleteAccount(jwt).data!!["canDeleteAccount"] as Boolean

fun operateReadAccount(jwt: String): GraphQlResponse = operateGraphQl(
    """
    query ReadAccount {
      readAccount {
        id
        username
        email
        firstName
        lastName
      }
    }
    """,
    jwt = jwt
)

fun readAccount(jwt: String): AccountInfo {
    val data = operateReadAccount(jwt).data!!["readAccount"] as Map<*, *>
    return jacksonObjectMapper.convertValue(data)
}

fun operateIsUsernameTaken(username: String): GraphQlResponse = operateGraphQl(
    """
    query IsUsernameTaken {
      isUsernameTaken(username: "$username")
    }
    """
)

fun isUsernameTaken(username: String): Boolean = operateIsUsernameTaken(username).data!!["isUsernameTaken"] as Boolean

fun operateIsEmailTaken(email: String): GraphQlResponse = operateGraphQl(
    """
    query IsEmailTaken {
      isEmailTaken(email: "$email")
    }
    """
)

fun isEmailTaken(email: String): Boolean = operateIsEmailTaken(email).data!!["isEmailTaken"] as Boolean

fun operateReadChats(jwt: String): GraphQlResponse = operateGraphQl(
    """
    query ReadChats {
      readChats {
        __typename
        id
        ... on GroupChat {
          title
          description
          adminId
          userIdList
        }
        ... on PrivateChat {
          userId
        }
      }
    }
    """,
    jwt = jwt
)

fun readChats(jwt: String): List<Chat> = (operateReadChats(jwt).data!!["readChats"] as List<*>).map(::convertChat)

fun operateSearchChats(query: String, jwt: String): GraphQlResponse = operateGraphQl(
    """
    query SearchChats {
      searchChats(query: "$query") {
        __typename
        id
        ... on GroupChat {
          title
          description
          adminId
          userIdList
        }
        ... on PrivateChat {
          userId
        }
      }
    }
    """,
    jwt = jwt
)

fun searchChats(query: String, jwt: String): List<Chat> =
    (operateSearchChats(query, jwt).data!!["searchChats"] as List<*>).map(::convertChat)

fun operateReadContacts(jwt: String): GraphQlResponse = operateGraphQl(
    """
    query ReadContacts {
      readContacts {
        id
        username
        email
        firstName
        lastName
      }
    }
    """,
    jwt = jwt
)

fun readContacts(jwt: String): List<AccountInfo> {
    val data = operateReadContacts(jwt).data!!["readContacts"] as List<*>
    return jacksonObjectMapper.convertValue(data)
}

fun operateSearchContacts(query: String, jwt: String): GraphQlResponse = operateGraphQl(
    """
    query SearchContacts {
      searchContacts(query: "$query") {
        id
        username
        email
        firstName
        lastName
      }
    }
    """,
    jwt = jwt
)

fun searchContacts(query: String, jwt: String): List<AccountInfo> {
    val data = operateSearchContacts(query, jwt).data!!["searchContacts"] as List<*>
    return jacksonObjectMapper.convertValue(data)
}

fun operateRequestJwt(login: Login): GraphQlResponse = operateGraphQl(
    """
    query RequestJwt(${"$"}login: Login!) {
      requestJwt(login: ${"$"}login) {
        jwt
        expiry
        refreshToken
        refreshTokenExpiry
      }
    }
    """,
    variables = mapOf("login" to login)
)

fun requestJwt(login: Login): AuthToken {
    val data = operateRequestJwt(login).data!!["requestJwt"] as Map<*, *>
    return jacksonObjectMapper.convertValue(data)
}

fun operateSearchUsers(query: String): GraphQlResponse = operateGraphQl(
    """
    query SearchUsers {
      searchUsers(query: "$query") {
        id
        username
        email
        firstName
        lastName
      }
    }
    """
)

fun searchUsers(query: String): List<AccountInfo> {
    val data = operateSearchUsers(query).data!!["searchUsers"] as List<*>
    return jacksonObjectMapper.convertValue(data)
}

class CanDeleteAccountTest : StringSpec({
    listener(AppListener())

    "An account should be able to be deleted if the user is the admin of an empty group chat" {
        val login = createVerifiedUsers(1)[0].login
        val jwt = requestJwt(login).jwt
        createGroupChat(NewGroupChat("Title", userIdList = setOf()), jwt)
        canDeleteAccount(jwt).shouldBeTrue()
    }

    "An account shouldn't be allowed to be deleted if the user is the admin of a nonempty group chat" {
        val (admin, user) = createVerifiedUsers(2)
        val jwt = requestJwt(admin.login).jwt
        createGroupChat(NewGroupChat("Title", userIdList = setOf(user.info.id)), jwt)
        canDeleteAccount(jwt).shouldBeFalse()
    }
})

class ReadAccountTest : StringSpec({
    listener(AppListener())

    "The user's account info should be returned" {
        val user = createVerifiedUsers(1)[0]
        val jwt = requestJwt(user.login).jwt
        readAccount(jwt) shouldBe user.info
    }
})

class IsUsernameTakenTest : StringSpec({
    listener(AppListener())

    "The username shouldn't be taken" { isUsernameTaken("username").shouldBeFalse() }

    "The username should be taken" {
        val (info) = createVerifiedUsers(1)[0]
        isUsernameTaken(info.username).shouldBeTrue()
    }
})

class IsEmailTakenTest : StringSpec({
    listener(AppListener())

    "The email shouldn't be taken" { isEmailTaken("username@example.com").shouldBeFalse() }

    "The email should be taken" {
        val (info) = createVerifiedUsers(1)[0]
        isEmailTaken(info.email).shouldBeTrue()
    }
})

class ReadChatsTest : StringSpec({
    listener(AppListener())

    fun createChats(admin: CreatedUser, user: CreatedUser): List<Chat> {
        val adminJwt = requestJwt(admin.login).jwt
        val adminGroupChat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val adminGroupChatId = createGroupChat(adminGroupChat, adminJwt)
        val userGroupChat = NewGroupChat("Title", userIdList = setOf(admin.info.id))
        val userGroupChatId = createGroupChat(userGroupChat, requestJwt(user.login).jwt)
        val createdPrivateChatId = createPrivateChat(user.info.id, adminJwt)
        val invitee = createVerifiedUsers(1)[0]
        val invitedPrivateChatId = createPrivateChat(admin.info.id, requestJwt(invitee.login).jwt)
        return listOf(
            with(adminGroupChat) {
                GroupChat(adminGroupChatId, admin.info.id, userIdList + admin.info.id, title, description)
            },
            with(userGroupChat) {
                GroupChat(userGroupChatId, user.info.id, userIdList + user.info.id, title, description)
            },
            PrivateChat(createdPrivateChatId, user.info.id),
            PrivateChat(invitedPrivateChatId, invitee.info.id)
        )
    }

    "Private and group chats the user made, was invited to, and was added to should be retrieved" {
        val (admin, user) = createVerifiedUsers(2)
        val chats = createChats(admin, user)
        readChats(requestJwt(admin.login).jwt) shouldBe chats
    }

    "Private chats deleted by the user shouldn't be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = requestJwt(users[0].login).jwt
        val chatId = createPrivateChat(users[1].info.id, jwt)
        deletePrivateChat(chatId, jwt)
        readChats(jwt).shouldBeEmpty()
    }

    "Group chats deleted by the user shouldn't be retrieved" {
        val (admin, user) = createVerifiedUsers(2)
        val jwt = requestJwt(admin.login).jwt
        val chat = NewGroupChat("Title", userIdList = setOf(user.info.id))
        val chatId = createGroupChat(chat, jwt)
        leaveGroupChat(jwt, chatId, newAdminId = user.info.id)
        readChats(jwt).shouldBeEmpty()
    }

    "Chats deleted only by the invitee should be retrieved" {
        val users = createVerifiedUsers(2)
        val jwt = requestJwt(users[0].login).jwt
        val inviteeId = users[1].info.id
        val chatId = createPrivateChat(inviteeId, jwt)
        deletePrivateChat(chatId, requestJwt(users[1].login).jwt)
        readChats(jwt) shouldBe listOf(PrivateChat(chatId, inviteeId))
    }
})

class SearchChatsTest : StringSpec({
    listener(AppListener())

    fun createPrivateChats(jwt: String): List<PrivateChat> = listOf(
        NewAccount(username = "Iron Man", password = "malibu", email = "tony@stark.com", firstName = "Tony"),
        NewAccount(username = "Iron Fist", password = "monk", email = "iron.fist@monks.org"),
        NewAccount(username = "Chris Tony", password = "pass", email = "chris@example.com", lastName = "Tony")
    ).map {
        createAccount(it)
        val userId = Auth.findUserByUsername(it.username).id
        val chatId = createPrivateChat(userId, jwt)
        PrivateChat(chatId, userId)
    }

    fun createGroupChats(adminId: String, jwt: String): List<GroupChat> = listOf(
        NewGroupChat("Iron Man Fan Club", userIdList = setOf()),
        NewGroupChat("Language Class", userIdList = setOf()),
        NewGroupChat("Programming Languages", userIdList = setOf()),
        NewGroupChat("Tony's Birthday", userIdList = setOf())
    ).map {
        val chatId = createGroupChat(it, jwt)
        GroupChat(chatId, adminId, it.userIdList + adminId, it.title, it.description)
    }

    "Private chats and group chats should be searched case-insensitively" {
        val user = createVerifiedUsers(1)[0]
        val jwt = requestJwt(user.login).jwt
        val privateChats = createPrivateChats(jwt)
        val groupChats = createGroupChats(user.info.id, jwt)
        searchChats("iron", jwt) shouldBe listOf(privateChats[0], privateChats[1], groupChats[0])
        searchChats("tony", jwt) shouldBe listOf(privateChats[0], privateChats[2], groupChats[3])
        searchChats("language", jwt) shouldBe listOf(groupChats[1], groupChats[2])
        searchChats("an f", jwt) shouldBe listOf(groupChats[0])
        searchChats("Harry Potter", jwt).shouldBeEmpty()
    }

    "A query which happens to match the user shouldn't return all the results" {
        val accounts = listOf(
            NewAccount(username = "john_doe", password = "pass", email = "john.doe@example.com", firstName = "John"),
            NewAccount("username", "password", "username@example.com")
        )
        for (account in accounts) createAccount(account)
        val response = with(accounts[0]) {
            Auth.verifyEmail(username)
            searchChats("John", requestJwt(Login(username, password)).jwt)
        }
        response.shouldBeEmpty()
    }
})

class ReadContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be read" {
        val (creator, contact1, contact2) = createVerifiedUsers(3)
        val jwt = requestJwt(creator.login).jwt
        createContacts(listOf(contact1.info.id, contact2.info.id), jwt)
        readContacts(jwt) shouldBe listOf(contact1.info, contact2.info)
    }
})

class SearchContactsTest : StringSpec({
    listener(AppListener())

    "Contacts should be searched case-insensitively" {
        val accounts = listOf(
            NewAccount(username = "john_doe", password = "p", email = "john.doe@example.com"),
            NewAccount(username = "john_roger", password = "p", email = "john.roger@example.com"),
            NewAccount(username = "nick_bostrom", password = "p", email = "nick.bostrom@example.com"),
            NewAccount(username = "iron_man_fan", password = "p", email = "roger@example.com", firstName = "John")
        ).map {
            createAccount(it)
            val userId = Auth.findUserByUsername(it.username).id
            AccountInfo(userId, it.username, it.email, it.firstName, it.lastName)
        }
        val jwt = requestJwt(createVerifiedUsers(1)[0].login).jwt
        createContacts(accounts.map { it.id }, jwt)
        searchContacts("john", jwt) shouldBe listOf(accounts[0], accounts[1], accounts[3])
        searchContacts("bost", jwt) shouldBe listOf(accounts[2])
        searchContacts("Roger", jwt) shouldBe listOf(accounts[1], accounts[3])
    }
})

class RequestJwtTest : StringSpec({
    listener(AppListener())

    "A token set should be sent" {
        requestJwt(createVerifiedUsers(1)[0].login) // Successfully calling the function tests the response body.
    }

    "A token set shouldn't be created for a nonexistent user" {
        operateRequestJwt(Login("username", "password")).errors!![0].message shouldBe NonexistentUserException().message
    }

    "A token set shouldn't be created for a user who hasn't verified their email" {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        operateRequestJwt(login).errors!![0].message shouldBe UnverifiedEmailException().message
    }

    "A token set shouldn't be created for an incorrect password" {
        val login = createVerifiedUsers(1)[0].login
        val response = operateRequestJwt(login.copy(password = "incorrect password"))
        response.errors!![0].message shouldBe IncorrectPasswordException().message
    }
})

class SearchUsersTest : StringSpec({
    listener(AppListener())

    /** Creates users, and returns their ID list. */
    fun createUsers(): List<AccountInfo> = listOf(
        NewAccount(username = "tony", password = "p", email = "tony@example.com", firstName = "Tony"),
        NewAccount(username = "johndoe", password = "p", email = "john@example.com", firstName = "John"),
        NewAccount(username = "john.rogers", password = "p", email = "rogers@example.com"),
        NewAccount(username = "anonymous", password = "p", email = "anon@example.com", firstName = "John")
    ).map {
        createAccount(it)
        val id = Auth.findUserByUsername(it.username).id
        AccountInfo(id, it.username, it.email, it.firstName, it.lastName)
    }

    "Users should be searched case-insensitively" {
        val infoList = createUsers()
        searchUsers("tOnY") shouldBe listOf(infoList[0])
        searchUsers("doe") shouldBe listOf(infoList.elementAt(1))
        searchUsers("john") shouldBe listOf(infoList[1], infoList[2], infoList[3])
    }
})

private fun convertChat(chat: Any?): Chat {
    chat as Map<*, *>
    return if (chat["__typename"] == "PrivateChat") jacksonObjectMapper.convertValue<PrivateChat>(chat)
    else jacksonObjectMapper.convertValue<GroupChat>(chat)
}