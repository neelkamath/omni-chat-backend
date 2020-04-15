package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.verifyEmail
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

fun refreshJwt(refreshToken: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "jwt-refresh") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        setBody(listOf("refresh_token" to refreshToken).formUrlEncode())
    }
}.response

class PostJwtRefreshTest : StringSpec({
    listener(AppListener())

    "A refresh token should issue a new token set" {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        Auth.verifyEmail(login.username)
        val token = gson.fromJson(requestJwt(login).content, AuthToken::class.java).refreshToken
        val response = refreshJwt(token)
        response.status() shouldBe HttpStatusCode.OK
        gson.fromJson(response.content, AuthToken::class.java) // Successfully parsing it verifies the response body.
    }
})