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

fun createUser(user: NewUser): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "user") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(gson.toJson(user))
    }
}.response

fun updateUser(update: UserUpdate, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "user") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(update))
        }
    }.response

fun readUser(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Get, "user") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun deleteUser(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Delete, "user") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun verifyEmail(email: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("email", email) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "email-verification?$parameters")
}.response

fun resetPassword(email: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("email", email) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "password-reset?$parameters")
}.response

class GetUserTest : StringSpec({
    listener(AppListener())

    "An account's details should be received" {
        val user = NewUser("username", "password", "john@example.com", firstName = "John")
        createUser(user)
        Auth.verifyEmail(user.username)
        val response = readUser(getJwt(Login(user.username, user.password)))
        response.status() shouldBe HttpStatusCode.OK
        with(gson.fromJson(response.content, UserInfo::class.java)) {
            username shouldBe user.username
            email shouldBe user.email
            firstName shouldBe user.firstName
            lastName shouldBe user.lastName
        }
    }
})

class PatchUserTest : StringSpec({
    listener(AppListener())

    fun testAccountInfo(user: NewUser, updatedUser: UserUpdate) {
        Auth.usernameExists(user.username).shouldBeFalse()
        with(Auth.findUserByUsername(updatedUser.username!!)) {
            username shouldBe updatedUser.username
            email shouldBe updatedUser.email
            isEmailVerified.shouldBeFalse()
            firstName shouldBe user.firstName
            lastName shouldBe updatedUser.lastName
        }
    }

    "An account's info should be updated" {
        val user = NewUser(username = "john_doe", password = "pass", email = "john.doe@example.com")
        createUser(user)
        Auth.verifyEmail(user.username)
        val updatedUser = UserUpdate(username = "john_rogers", email = "john.rogers@example.com", lastName = "Rogers")
        updateUser(updatedUser, getJwt(Login(user.username, user.password))).status() shouldBe HttpStatusCode.NoContent
        testAccountInfo(user, updatedUser)
    }

    "The password should be updated" {
        val user = NewUser("username", "password", "username@example.com")
        createUser(user)
        Auth.verifyEmail(user.username)
        val newPassword = "new password"
        val jwt = getJwt(Login(user.username, user.password))
        updateUser(UserUpdate(password = newPassword), jwt)
        requestJwt(Login(user.username, newPassword)).status() shouldBe HttpStatusCode.OK
    }

    "Updating a username to one already taken shouldn't allow the account to be updated" {
        val user1Login = Login("username1", "password")
        val user1 = NewUser(user1Login.username, user1Login.password, "username1@example.com")
        createUser(user1)
        Auth.verifyEmail(user1Login.username)
        val user2Username = "username2"
        val user2 = NewUser(user2Username, "password", "username2@example.com")
        createUser(user2)
        val updatedUser = UserUpdate(user2Username)
        val response = updateUser(updatedUser, getJwt(user1Login))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.USERNAME_TAKEN)
    }

    "Updating an email to one already taken shouldn't allow the account to be updated" {
        val email = "username1@example.com"
        createUser(NewUser("username1", "password", email))
        val user = NewUser("username2", "password", "username2@example.com")
        createUser(user)
        Auth.verifyEmail(user.username)
        val jwt = getJwt(Login(user.username, user.password))
        val response = updateUser(UserUpdate(email = email), jwt)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.EMAIL_TAKEN)
    }
})

class PostUserTest : StringSpec({
    listener(AppListener())

    "An account should be created" {
        val user = NewUser("username", "password", "username@example.com")
        createUser(user).status() shouldBe HttpStatusCode.Created
        with(Auth.findUserByUsername(user.username)) {
            username shouldBe user.username
            email shouldBe user.email
        }
    }

    "An account with an already taken username shouldn't be created" {
        val user = NewUser("username", "password", "username@example.com")
        createUser(user)
        val response = createUser(user)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.USERNAME_TAKEN)
    }

    "An account with an already taken email shouldn't be created" {
        val email = "username@example.com"
        createUser(NewUser("username1", "password", email))
        val response = createUser(NewUser("username2", "password", email))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.EMAIL_TAKEN)
    }
})

class DeleteUserTest : StringSpec({
    listener(AppListener())

    "An account should be deleted" {
        val login = Login("username", "password")
        createUser(NewUser(login.username, login.password, "username@example.com"))
        Auth.verifyEmail(login.username)
        deleteUser(getJwt(login)).status() shouldBe HttpStatusCode.NoContent
        Auth.usernameExists(login.username).shouldBeFalse()
    }
})

class GetEmailVerificationTest : StringSpec({
    listener(AppListener())

    "Sending a verification email should respond with an HTTP status code of 204" {
        val email = "username@example.com"
        createUser(NewUser("username", "password", email))
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
        createUser(NewUser("username", "password", email))
        resetPassword(email).status() shouldBe HttpStatusCode.NoContent
    }

    "Requesting a password reset for an unregistered address should respond with an HTTP status code of 400" {
        resetPassword("username@example.com").status() shouldBe HttpStatusCode.BadRequest
    }
})