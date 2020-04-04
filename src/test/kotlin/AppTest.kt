package com.neelkamath.omniChat.test

import com.neelkamath.omniChat.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication

/** Tests for the `/user` endpoint. */
class UserTest : StringSpec({
    val newUser = User(username = "johndoe", password = "pass", email = "johndoe@example.com")

    beforeTest { setUpAuth() }

    afterTest { realm.remove() }

    fun createUser(user: User = newUser): TestApplicationCall = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Post, "/user") {
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(gson.toJson(user))
        }
    }

    "Creating an account with an unused username should respond with an HTTP status code of 201" {
        createUser().response.status() shouldBe HttpStatusCode.Created
    }

    "Creating an account with an unused username should result in the account being created" {
        createUser()
        val users = realm.users().search(newUser.username)
        users shouldHaveSize 1
        with(users[0]) {
            username shouldBe newUser.username
            email shouldBe newUser.email
        }
    }

    "Creating an account with a used username should respond with an HTTP status code of 400" {
        createUser()
        createUser().response.status() shouldBe HttpStatusCode.BadRequest
    }

    "Creating an account with a used username should cause the account to not be created" {
        createUser()
        createUser()
        realm.users().search(newUser.username) shouldHaveSize 1
    }
})

/** Tests for the `/health_check` endpoint. */
class HealthCheckTest : StringSpec({
    "A health check should respond with an HTTP status code of 204" {
        withTestApplication(Application::main) {
            handleRequest(HttpMethod.Get, "/health_check").response.status() shouldBe HttpStatusCode.NoContent
        }
    }
})