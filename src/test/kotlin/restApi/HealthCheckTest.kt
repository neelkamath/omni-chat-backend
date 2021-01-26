package com.neelkamath.omniChat.restApi

import com.neelkamath.omniChat.main
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals

private fun getHealthCheck(): TestApplicationResponse =
    withTestApplication(Application::main) { handleRequest(HttpMethod.Get, "health-check") }.response

class HealthCheckTest {
    @Nested
    inner class GetHealthCheck {
        @Test
        fun `A health check must respond with an HTTP status code of 204`() {
            assertEquals(HttpStatusCode.NoContent, getHealthCheck().status())
        }
    }
}
