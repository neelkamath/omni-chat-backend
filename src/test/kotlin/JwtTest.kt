package com.neelkamath.omniChat

import com.auth0.jwt.JWT
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.date.shouldBeBefore
import java.sql.Timestamp
import java.time.LocalDateTime

class JwtTest : FunSpec(body)

private val body: FunSpec.() -> Unit = {
    /** Tests that the [actual] [LocalDateTime] is within five seconds of the [expected] [LocalDateTime]. */
    fun testDateTime(actual: LocalDateTime, expected: LocalDateTime) {
        val leewayInSeconds = 5L
        actual shouldBeAfter expected.minusSeconds(leewayInSeconds)
        actual shouldBeBefore expected.plusSeconds(leewayInSeconds)
    }

    fun readExpiry(token: String): LocalDateTime = JWT.decode(token).expiresAt.run { Timestamp(time).toLocalDateTime() }

    test("The access and refresh tokens should expire in one hour and one week respectively") {
        val (accessToken, refreshToken) = buildAuthToken("user ID")
        val now = LocalDateTime.now()
        testDateTime(actual = readExpiry(accessToken), expected = now.plusHours(1))
        testDateTime(actual = readExpiry(refreshToken), expected = now.plusWeeks(1))
    }
}