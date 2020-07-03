package com.neelkamath.omniChat

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationResponse

/** Asserts a [TestApplicationResponse] has a [TestApplicationResponse.status] of [HttpStatusCode.Unauthorized]. */
fun TestApplicationResponse.shouldHaveUnauthorizedStatus(): Unit = this should haveUnauthorizedStatus()

/** Asserts a [TestApplicationResponse] has a [TestApplicationResponse.status] of [HttpStatusCode.Unauthorized]. */
fun TestApplicationResponse.shouldNotHaveUnauthorizedStatus(): Unit = this shouldNot haveUnauthorizedStatus()

/** Asserts a [TestApplicationResponse] has a [TestApplicationResponse.status] of [HttpStatusCode.Unauthorized]. */
private fun haveUnauthorizedStatus() = object : Matcher<TestApplicationResponse> {
    override fun test(value: TestApplicationResponse) = MatcherResult(
        value.status()!! == HttpStatusCode.Unauthorized,
        "TestApplicationResponse $value should have a status code of 401",
        "TestApplicationResponse $value should not have a status code of 401"
    )
}