package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

class GetUserTest : StringSpec({
    listener(AuthListener())

    "An account's details should be received" {
        val user = User(Login("john", "password"), "john@gmail.com", firstName = "John")
        Server.createAccount(user)
        Auth.verifyEmail(user.login!!.username!!)
        val jwt = gson.fromJson(Server.requestJwt(user.login!!).content, AuthToken::class.java).jwt
        val response = Server.readAccount(jwt)
        response.status() shouldBe HttpStatusCode.OK
        with(gson.fromJson(response.content, UserDetails::class.java)) {
            username shouldBe user.login!!.username
            email shouldBe user.email
            firstName shouldBe user.firstName
            lastName shouldBe user.lastName
        }
    }
})

class PatchUserTest : StringSpec({
    listener(AuthListener())

    fun testAccountUpdate(user: User, updatedUser: User) {
        Auth.userExists(user.login!!.username!!).shouldBeFalse()
        with(Auth.findUser(updatedUser.login!!.username!!)) {
            username shouldBe updatedUser.login!!.username
            email shouldBe updatedUser.email
            isEmailVerified.shouldBeFalse()
            firstName shouldBe user.firstName
            lastName shouldBe updatedUser.lastName
        }
    }

    "An account should update" {
        val user = User(Login(username = "john_doe", password = "pass"), email = "john.doe@example.com")
        Server.createAccount(user)
        Auth.verifyEmail(user.login!!.username!!)
        val updatedUser = User(Login(username = "john_rogers"), email = "john.rogers@example.com", lastName = "Rogers")
        val jwt = gson.fromJson(Server.requestJwt(user.login!!).content, AuthToken::class.java).jwt
        Server.updateAccount(updatedUser, jwt).status() shouldBe HttpStatusCode.NoContent
        testAccountUpdate(user, updatedUser)
    }

    "Updating a username to one already taken shouldn't allow the account to be updated" {
        val user1Login = Login("username1", "password")
        val user1 = User(user1Login, "username1@gmail.com")
        Server.createAccount(user1)
        Auth.verifyEmail(user1Login.username!!)
        val user2Username = "username2"
        val user2 = User(Login(user2Username, "password"), "username2@gmail.com")
        Server.createAccount(user2)
        val jwt = gson.fromJson(Server.requestJwt(user1Login).content, AuthToken::class.java).jwt
        val updatedUser = user1.copy(user1Login.copy(user2Username))
        val response = Server.updateAccount(updatedUser, jwt)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.USERNAME_TAKEN)
    }
})

class PostUserTest : StringSpec({
    listener(AuthListener())

    "An account should be created" {
        val user = User(Login("username", "password"), "username@gmail.com")
        Server.createAccount(user).status() shouldBe HttpStatusCode.Created
        with(Auth.findUser(user.login!!.username!!)) {
            username shouldBe user.login!!.username
            email shouldBe user.email
        }
    }

    "An account with an already taken username shouldn't be created" {
        val user = User(Login("username", "password"), "username@gmail.com")
        Server.createAccount(user)
        val response = Server.createAccount(user)
        response.status() shouldBe HttpStatusCode.BadRequest
        gson.fromJson(response.content, InvalidUser::class.java) shouldBe InvalidUser(InvalidUserReason.USERNAME_TAKEN)
    }
})