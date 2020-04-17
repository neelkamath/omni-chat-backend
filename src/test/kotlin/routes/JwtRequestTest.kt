package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
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

fun requestJwt(login: Login): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "jwt-request") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(gson.toJson(login))
    }
}.response

class PostJwtRequestTest : StringSpec({
    listener(AppListener())

    "A token set should be sent" {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        Auth.verifyEmail(login.username)
        val response = requestJwt(login)
        response.status() shouldBe HttpStatusCode.OK
        gson.fromJson(response.content, AuthToken::class.java) // Successfully parsing it verifies the response body.
    }

    "A token set shouldn't be created for a nonexistent user" {
        val response = requestJwt(Login("username", "password"))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.NONEXISTENT_USER)
    }

    "A token set shouldn't be created for a user who hasn't verified their email" {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        val response = requestJwt(login)
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.EMAIL_NOT_VERIFIED)
    }

    "A token set shouldn't be created for an incorrect password" {
        val username = "username"
        createAccount(NewAccount(username, "correct_password", "username@example.com"))
        Auth.verifyEmail(username)
        val response = requestJwt(Login(username, "incorrect_password"))
        response.status() shouldBe HttpStatusCode.BadRequest
        val body = gson.fromJson(response.content, InvalidAccount::class.java)
        body shouldBe InvalidAccount(InvalidAccountReason.INCORRECT_PASSWORD)
    }
})