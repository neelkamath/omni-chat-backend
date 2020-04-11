package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

fun createUser(user: NewUser): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "/user") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(gson.toJson(user))
    }
}.response

fun updateUser(update: UserUpdate, jwt: String): TestApplicationResponse =
    withTestApplication(Application::main) {
        handleRequest(HttpMethod.Patch, "/user") {
            addHeader(HttpHeaders.Authorization, "Bearer $jwt")
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(update))
        }
    }.response

fun readUser(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Get, "/user") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

fun deleteUser(jwt: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Delete, "/user") { addHeader(HttpHeaders.Authorization, "Bearer $jwt") }
}.response

class GetUserTest : StringSpec({
    listener(AppListener())

    "An account's details should be received" {
        val user = NewUser("username", "password", "john@gmail.com", firstName = "John")
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

    fun testAccountUpdate(user: NewUser, updatedUser: UserUpdate) {
        Auth.usernameExists(user.username).shouldBeFalse()
        with(Auth.findUserByUsername(updatedUser.username!!)) {
            username shouldBe updatedUser.username
            email shouldBe updatedUser.email
            isEmailVerified.shouldBeFalse()
            firstName shouldBe user.firstName
            lastName shouldBe updatedUser.lastName
        }
    }

    "An account should update" {
        val user = NewUser(username = "john_doe", password = "pass", email = "john.doe@gmail.com")
        createUser(user)
        Auth.verifyEmail(user.username)
        val updatedUser = UserUpdate(username = "john_rogers", email = "john.rogers@gmail.com", lastName = "Rogers")
        updateUser(updatedUser, getJwt(Login(user.username, user.password))).status() shouldBe HttpStatusCode.NoContent
        testAccountUpdate(user, updatedUser)
    }

    "Updating a username to one already taken shouldn't allow the account to be updated" {
        val user1Login = Login("username1", "password")
        val user1 = NewUser(user1Login.username, user1Login.password, "username1@gmail.com")
        createUser(user1)
        Auth.verifyEmail(user1Login.username)
        val user2Username = "username2"
        val user2 = NewUser(user2Username, "password", "username2@gmail.com")
        createUser(user2)
        val updatedUser = UserUpdate(user2Username)
        val response = updateUser(updatedUser, getJwt(user1Login))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.USERNAME_TAKEN)
    }
})

class PostUserTest : StringSpec({
    listener(AppListener())

    "An account should be created" {
        val user = NewUser("username", "password", "username@gmail.com")
        createUser(user).status() shouldBe HttpStatusCode.Created
        with(Auth.findUserByUsername(user.username)) {
            username shouldBe user.username
            email shouldBe user.email
        }
    }

    "An account with an already taken username shouldn't be created" {
        val user = NewUser("username", "password", "username@gmail.com")
        createUser(user)
        val response = createUser(user)
        response.status() shouldBe HttpStatusCode.BadRequest
        gson.fromJson(response.content, InvalidUser::class.java) shouldBe InvalidUser(InvalidUserReason.USERNAME_TAKEN)
    }
})

class DeleteUserTest : StringSpec({
    listener(AppListener())

    "An account should be deleted" {
        val login = Login("username", "password")
        createUser(NewUser(login.username, login.password, "username@gmail.com"))
        Auth.verifyEmail(login.username)
        deleteUser(getJwt(login)).status() shouldBe HttpStatusCode.NoContent
        Auth.usernameExists(login.username).shouldBeFalse()
    }
})