package com.neelkamath.omnichat.test

import com.neelkamath.omnichat.main
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

class AppTest : StringSpec({
    "A health check should respond with an HTTP 204" {
        withTestApplication(Application::main) {
            handleRequest(HttpMethod.Get, "/health_check").response.status() shouldBe HttpStatusCode.NoContent
        }
    }
})