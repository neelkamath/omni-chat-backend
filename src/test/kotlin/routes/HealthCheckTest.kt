package com.neelkamath.omniChat.test.routes

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

class GetHealthCheckTest : StringSpec({
    "A health check should respond with an HTTP status code of 204" {
        Server.checkHealth().status() shouldBe HttpStatusCode.NoContent
    }
})