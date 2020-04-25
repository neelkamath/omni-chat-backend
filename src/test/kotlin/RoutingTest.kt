package com.neelkamath.omniChat.test

import com.fasterxml.jackson.module.kotlin.readValue
import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.test.graphql.createAccount
import com.neelkamath.omniChat.test.graphql.requestJwt
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

fun checkHealth(): TestApplicationResponse =
    withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }.response

fun refreshJwt(refreshToken: String): TestApplicationResponse = withTestApplication(Application::main) {
    handleRequest(HttpMethod.Post, "jwt-refresh") {
        addHeader(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
        setBody(listOf("refresh_token" to refreshToken).formUrlEncode())
    }
}.response

class GetHealthCheckTest : StringSpec({
    "A health check should respond with an HTTP status code of 204" {
        checkHealth().status() shouldBe HttpStatusCode.NoContent
    }
})

class PostJwtRefreshTest : StringSpec({
    listener(AppListener())

    "A refresh token should issue a new token set" {
        val login = Login("username", "password")
        createAccount(NewAccount(login.username, login.password, "username@example.com"))
        Auth.verifyEmail(login.username)
        val token = requestJwt(login).refreshToken
        val response = refreshJwt(token)
        response.status() shouldBe HttpStatusCode.OK
        jacksonObjectMapper.readValue<AuthToken>(response.content!!) // Successfully parsing it tests the response body.
    }
})