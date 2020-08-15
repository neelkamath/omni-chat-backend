package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.test
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals

private fun getHealthCheck(): TestApplicationResponse =
    withTestApplication(Application::test) { handleRequest(HttpMethod.Get, "health-check") }.response

class HealthCheckTest {
    @Nested
    inner class GetHealthCheck {
        @Test
        fun `A health check should respond with an HTTP status code of 204`() {
            assertEquals(HttpStatusCode.NoContent, getHealthCheck().status())
        }
    }
}