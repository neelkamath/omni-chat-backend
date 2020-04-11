package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.main
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

fun checkHealth(): TestApplicationResponse =
    withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }.response

class GetHealthCheckTest : StringSpec({
    "A health check should respond with an HTTP status code of 204" {
        checkHealth().status() shouldBe HttpStatusCode.NoContent
    }
})