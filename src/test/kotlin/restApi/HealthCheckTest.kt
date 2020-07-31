package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.main
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication

private fun getHealthCheck(): TestApplicationResponse =
    withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }.response

class HealthCheckTest : FunSpec({
    context("getHealthCheck(Routing)") {
        test("A health check should respond with an HTTP status code of 204") {
            getHealthCheck().status() shouldBe HttpStatusCode.NoContent
        }
    }
})