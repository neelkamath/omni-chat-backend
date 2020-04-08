package com.neelkamath.omniChat.routes

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

class PostJwtTest : StringSpec({
    listener(AuthListener())

    "A token set should be sent" {
        val login = Login("username", "password")
        Server.createAccount(User(login, "username@gmail.com"))
        Auth.verifyEmail(login.username!!)
        val response = Server.requestJwt(login)
        response.status() shouldBe HttpStatusCode.OK
        gson.fromJson(response.content, AuthToken::class.java) // Successfully parsing it verifies the response body.
    }

    "A token set shouldn't be created for a nonexistent user" {
        val response = Server.requestJwt(Login("username", "password"))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.NONEXISTENT_USER)
    }

    "A token set shouldn't be created for a user who hasn't verified their email" {
        val login = Login("username", "password")
        Server.createAccount(User(login, "username@gmail.com"))
        val response = Server.requestJwt(login)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.EMAIL_NOT_VERIFIED)
    }

    "A token set shouldn't be created for an incorrect password" {
        val username = "username"
        Server.createAccount(User(Login(username, "correct_password"), "username@gmail.com"))
        Auth.verifyEmail(username)
        val response = Server.requestJwt(Login(username, "incorrect_password"))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidUser::class.java)
        body shouldBe InvalidUser(InvalidUserReason.INCORRECT_PASSWORD)
    }
})