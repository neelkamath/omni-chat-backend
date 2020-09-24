package com.neelkamath.omniChat

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.neelkamath.omniChat.db.tables.OnetimeTokens
import com.neelkamath.omniChat.db.tables.read
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import java.sql.Timestamp
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        fun `The access and refresh tokens should expire in one hour and one week respectively`() {
            val (accessToken, refreshToken) = buildTokenSet(userId = 1)
            val now = LocalDateTime.now()
            testDateTime(actual = readExpiry(accessToken), expected = now.plusHours(1))
            testDateTime(actual = readExpiry(refreshToken), expected = now.plusWeeks(1))
        }

        @Test
        fun `Onetime tokens should have additional claims, and a DB entry`() {
            val token = buildOnetimeToken(userId = 1)
            val jwt = JWT.decode(token)
            assertTrue(jwt.getClaim("onetime").asBoolean())
            assertEquals(OnetimeTokens.read()[0], jwt.id.toInt())
        }
    }

    @Nested
    inner class IsInvalidOnetimeToken {
        private fun isInvalidOnetimeToken(token: String): Boolean = isInvalidOnetimeToken(JWT.decode(token))

        @Test
        fun `A token without a "onetime" claim shouldn't be invalid`() {
            val token = buildTokenSet(userId = 1).accessToken
            assertFalse(isInvalidOnetimeToken(token))
        }

        @Test
        fun `A token with a "false" "onetime" claim shouldn't be invalid`() {
            val token = JWT
                .create()
                .withClaim("onetime", false)
                .withJWTId(OnetimeTokens.create().toString())
                .sign(Algorithm.HMAC256("secret"))
            assertFalse(isInvalidOnetimeToken(token))
        }

        @Test
        fun `A token shouldn't be invalid if its "jti" claim exists in the DB`() {
            val token = buildOnetimeToken(userId = 1)
            assertFalse(isInvalidOnetimeToken(token))
        }

        @Test
        fun `A token should be invalid if its "jti" claim doesn't exist in the DB`() {
            val token = buildOnetimeToken(userId = 1)
            transaction { OnetimeTokens.deleteAll() }
            assertTrue(isInvalidOnetimeToken(token))
        }
    }
}
