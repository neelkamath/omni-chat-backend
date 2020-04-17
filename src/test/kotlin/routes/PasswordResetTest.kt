package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.main
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun resetPassword(email: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("email", email) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "password-reset?$parameters")
}.response

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