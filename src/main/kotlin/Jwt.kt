package com.neelkamath.omniChat

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.impl.NullClaim
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import com.neelkamath.omniChat.db.tables.OnetimeTokens
import com.neelkamath.omniChat.graphql.routing.TokenSet
import io.ktor.auth.jwt.*
import java.sql.Timestamp
import java.time.LocalDateTime

private val algorithm: Algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET"))
val jwtVerifier: JWTVerifier = JWT.require(algorithm).build()

/**
 * The [TokenSet.accessToken]'s subject will be the [userId]. The [TokenSet.accessToken]'s expiry and
 * [TokenSet.refreshToken]'s expiry will be set to one hour and one week respectively.
 */
fun buildTokenSet(userId: Int): TokenSet {
    val now = LocalDateTime.now()
    val accessToken = build(userId, getAccessTokenExpiry())
    val refreshToken = build(userId, expiry = now.plusWeeks(1))
    return TokenSet(accessToken, refreshToken)
}

/**
 * Returns an access token which expires in one hour meant for onetime use. The [userId] will be the subject, a claim
 * `"onetime"` will be `true`, and the `"jti"` claim will be an entry in [OnetimeTokens].
 */
fun buildOnetimeToken(userId: Int): String = build(userId, getAccessTokenExpiry(), onetime = true)

/** An access token's expiry: one hour from now. */
private fun getAccessTokenExpiry(): LocalDateTime = LocalDateTime.now().plusHours(1)

/**
 * If the token is only meant to be used once, set [onetime] to `true` so that a `"onetime"` claim will be present, the
 * `"jti"` claim will be a unique integer, and the token will be [OnetimeTokens.create]d for future reference.
 */
private fun build(userId: Int, expiry: LocalDateTime, onetime: Boolean = false): String = JWT
        .create()
        .withExpiresAt(Timestamp.valueOf(expiry))
        .withSubject(userId.toString())
        .apply {
            if (onetime) {
                withClaim("onetime", true)
                val id = OnetimeTokens.create()
                withJWTId(id.toString())
            }
        }
        .sign(algorithm)

/**
 * Returns:
 * - `false` if the [jwt] doesn't have a `"onetime"` claim.
 * - `false` if the [jwt]'s `"onetime"` claim is `false`.
 * - `false` if the `"jti"` claim exists in [OnetimeTokens].
 * - `true` if the `"jti"` claim doesn't exist in [OnetimeTokens].
 */
fun isInvalidOnetimeToken(jwt: DecodedJWT): Boolean = isInvalidOnetimeToken(jwt.getClaim("onetime"), jwt.id)

fun isInvalidOnetimeToken(jwt: JWTCredential): Boolean =
    isInvalidOnetimeToken(jwt.payload.getClaim("onetime"), jwt.payload.id)

private fun isInvalidOnetimeToken(onetimeClaim: Claim, jtiClaim: String?): Boolean = onetimeClaim !is NullClaim &&
        onetimeClaim.asBoolean() &&
        jtiClaim != null &&
        !OnetimeTokens.exists(jtiClaim.toInt())
