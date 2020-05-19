package com.neelkamath.omniChat

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.keycloak.representations.idm.UserRepresentation
import java.sql.Timestamp
import java.time.LocalDateTime

private val algorithm: Algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET"))

fun buildVerifier(): JWTVerifier = JWT.require(algorithm).build()

/**
 * The [TokenSet.accessToken]'s `sub` will be the [userId] (a [UserRepresentation.id]). The [TokenSet.accessToken]'s
 * expiry and [TokenSet.refreshToken]'s expiry will be set to one hour and one week respectively.
 */
fun buildAuthToken(userId: String): TokenSet {
    val now = LocalDateTime.now()
    val accessToken = build(userId, now.plusHours(1))
    val refreshToken = build(userId, now.plusWeeks(1))
    return TokenSet(accessToken, refreshToken)
}

/** The [userId] is a [UserRepresentation.id]. */
private fun build(userId: String, expiry: LocalDateTime): String {
    val date = Timestamp.valueOf(expiry)
    return JWT.create().withExpiresAt(date).withSubject(userId).sign(algorithm)
}