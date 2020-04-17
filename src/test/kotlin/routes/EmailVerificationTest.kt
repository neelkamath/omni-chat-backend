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

fun verifyEmail(email: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("email", email) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "email-verification?$parameters")
}.response

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