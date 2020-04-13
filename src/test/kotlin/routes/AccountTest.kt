package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

fun createAccount(account: NewAccount): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "account") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(gson.toJson(account))
    }
}.response

fun updateAccount(update: AccountUpdate, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "account") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(update))
        }
    }.response

fun readAccount(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Get, "account") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun deleteAccount(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Delete, "account") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun verifyEmail(email: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("email", email) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "email-verification?$parameters")
}.response

fun resetPassword(email: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("email", email) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "password-reset?$parameters")
}.response

class GetAccountTest : StringSpec({
    listener(AppListener())

    "An account's details should be received" {
        val user = NewAccount("username", "password", "john@example.com", firstName = "John")
        createAccount(user)
        Auth.verifyEmail(user.username)
        val response = readAccount(getJwt(Login(user.username, user.password)))
        response.status() shouldBe HttpStatusCode.OK
        with(gson.fromJson(response.content, AccountInfo::class.java)) {
            username shouldBe user.username
            email shouldBe user.email
            firstName shouldBe user.firstName
            lastName shouldBe user.lastName
        }
    }
})

class PatchAccountTest : StringSpec({
    listener(AppListener())

    fun testAccountInfo(account: NewAccount, updatedAccount: AccountUpdate) {
        Auth.usernameExists(account.username).shouldBeFalse()
        with(Auth.findUserByUsername(updatedAccount.username!!)) {
            username shouldBe updatedAccount.username
            email shouldBe updatedAccount.email
            isEmailVerified.shouldBeFalse()
            firstName shouldBe account.firstName
            lastName shouldBe updatedAccount.lastName
        }
    }

    "An account's info should be updated" {
        val user = NewAccount(username = "john_doe", password = "pass", email = "john.doe@example.com")
        createAccount(user)
        Auth.verifyEmail(user.username)
        val updatedUser =
            AccountUpdate(username = "john_rogers", email = "john.rogers@example.com", lastName = "Rogers")
        updateAccount(
            updatedUser,
            getJwt(Login(user.username, user.password))
        ).status() shouldBe HttpStatusCode.NoContent
        testAccountInfo(user, updatedUser)
    }

    "The password should be updated" {
        val user = NewAccount("username", "password", "username@example.com")
        createAccount(user)
        Auth.verifyEmail(user.username)
        val newPassword = "new password"
        val jwt = getJwt(Login(user.username, user.password))
        updateAccount(AccountUpdate(password = newPassword), jwt)
        requestJwt(Login(user.username, newPassword)).status() shouldBe HttpStatusCode.OK
    }

    "Updating a username to one already taken shouldn't allow the account to be updated" {
        val user1Login = Login("username1", "password")
        val user1 = NewAccount(user1Login.username, user1Login.password, "username1@example.com")
        createAccount(user1)
        Auth.verifyEmail(user1Login.username)
        val user2Username = "username2"
        val user2 = NewAccount(user2Username, "password", "username2@example.com")
        createAccount(user2)
        val updatedUser = AccountUpdate(user2Username)
        val response = updateAccount(updatedUser, getJwt(user1Login))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.USERNAME_TAKEN)
    }

    "Updating an email to one already taken shouldn't allow the account to be updated" {
        val email = "username1@example.com"
        createAccount(NewAccount("username1", "password", email))
        val user = NewAccount("username2", "password", "username2@example.com")
        createAccount(user)
        Auth.verifyEmail(user.username)
        val jwt = getJwt(Login(user.username, user.password))
        val response = updateAccount(AccountUpdate(email = email), jwt)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.EMAIL_TAKEN)
    }
})

class PostAccountTest : StringSpec({
    listener(AppListener())

    "An account should be created" {
        val user = NewAccount("username", "password", "username@example.com")
        createAccount(user).status() shouldBe HttpStatusCode.Created
        with(Auth.findUserByUsername(user.username)) {
            username shouldBe user.username
            email shouldBe user.email
        }
    }

    "An account with an already taken username shouldn't be created" {
        val user = NewAccount("username", "password", "username@example.com")
        createAccount(user)
        val response = createAccount(user)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.USERNAME_TAKEN)
    }

    "An account with an already taken email shouldn't be created" {
        val email = "username@example.com"
        createAccount(NewAccount("username1", "password", email))
        val response = createAccount(NewAccount("username2", "password", email))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.EMAIL_TAKEN)
    }
})

class DeleteAccountTest : StringSpec({
    listener(AppListener())

    "An account should be deleted" {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        Auth.verifyEmail(login.username)
        deleteAccount(getJwt(login)).status() shouldBe HttpStatusCode.NoContent
        Auth.usernameExists(login.username).shouldBeFalse()
    }
})

class GetEmailVerificationTest : StringSpec({
    listener(AppListener())

    "Sending a verification email should respond with an HTTP status code of 204" {
        val email = "username@example.com"
        createAccount(NewAccount("username", "password", email))
        verifyEmail(email).status() shouldBe HttpStatusCode.NoContent
    }

    "Sending a verification email to an unregistered address should respond with an HTTP status code of 400" {
        verifyEmail("username@example.com").status() shouldBe HttpStatusCode.BadRequest
    }
})

class GetPasswordResetTest : StringSpec({
    listener(AppListener())

    "Requesting a password reset should respond with an HTTP status code of 204" {
        val email = "username@example.com"
        createAccount(NewAccount("username", "password", email))
        resetPassword(email).status() shouldBe HttpStatusCode.NoContent
    }

    "Requesting a password reset for an unregistered address should respond with an HTTP status code of 400" {
        resetPassword("username@example.com").status() shouldBe HttpStatusCode.BadRequest
    }
})