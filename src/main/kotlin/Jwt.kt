package com.neelkamath.omniChatBackend

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.neelkamath.omniChatBackend.graphql.routing.TokenSet
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
    val accessToken = build(userId, expiry = now.plusHours(1))
    val refreshToken = build(userId, expiry = now.plusWeeks(1))
    return TokenSet(accessToken, refreshToken)
}

private fun build(userId: Int, expiry: LocalDateTime): String =
    JWT.create().withExpiresAt(Timestamp.valueOf(expiry)).withSubject(userId.toString()).sign(algorithm)
