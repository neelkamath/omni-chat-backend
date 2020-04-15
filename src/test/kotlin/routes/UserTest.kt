package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.*
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

fun readUser(userId: String): TestApplicationResponse = withTestApplication(Application::main) {
    val parameters = Parameters.build { append("user_id", userId) }.formUrlEncode()
    handleRequest(HttpMethod.Get, "user?$parameters")
}.response

class GetUserTest : StringSpec({
    listener(AppListener())

    "A user's info should be retrieved" {
        val account = NewAccount("username", "password", "username@example.com", "first name")
        createAccount(account)
        val response = readUser(Auth.findUserByUsername(account.username).id)
        response.status() shouldBe HttpStatusCode.OK
        val body = gson.fromJson(response.content, User::class.java)
        body shouldBe User(account.username, account.email, account.firstName, account.lastName)
    }

    "Requesting the details of a nonexistent user should respond with an HTTP status code of 400" {
        readUser("a nonexistent user ID").status() shouldBe HttpStatusCode.BadRequest
    }
})