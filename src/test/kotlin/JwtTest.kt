package com.neelkamath.omniChatBackend

import com.auth0.jwt.JWT
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertTrue

@ExtendWith(DbExtension::class)
class JwtTest {
    @Nested
    inner class Build {
        /** Tests that the [actual] [LocalDateTime] is within five seconds of the [expected] [LocalDateTime]. */
        private fun testDateTime(actual: LocalDateTime, expected: LocalDateTime) {
            val leewayInSeconds = 5L
            assertTrue(actual.isAfter(expected.minusSeconds(leewayInSeconds)))
            assertTrue(actual.isBefore(expected.plusSeconds(leewayInSeconds)))
        }

        private fun readExpiry(token: String): LocalDateTime =
            JWT.decode(token).expiresAt.run { Timestamp(time).toLocalDateTime() }

        @Test
        fun `The access and refresh tokens must expire in one hour and one week respectively`() {
            val (accessToken, refreshToken) = buildTokenSet(userId = 1)
            val now = LocalDateTime.now()
            testDateTime(actual = readExpiry(accessToken.value), expected = now.plusHours(1))
            testDateTime(actual = readExpiry(refreshToken.value), expected = now.plusWeeks(1))
        }
    }
}
