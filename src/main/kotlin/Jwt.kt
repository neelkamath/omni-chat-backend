package com.neelkamath.omniChat

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.keycloak.representations.AccessTokenResponse
import org.keycloak.representations.idm.UserRepresentation
import java.lang.System.currentTimeMillis
import java.util.*

object Jwt {
    const val audience = "omni-chat"
    private val issuer: String = System.getenv("KEYCLOAK_URL")
    private val algorithm: Algorithm = Algorithm.HMAC256(System.getenv("JWT_SECRET"))

    fun buildVerifier(): JWTVerifier = JWT.require(algorithm).withAudience(audience).withIssuer(issuer).build()

    /** The [AuthToken.jwt]'s `sub` will be the [userId] (a [UserRepresentation.id]). */
    fun buildAuthToken(userId: String, token: AccessTokenResponse): AuthToken {
        val expiry = Date(currentTimeMillis() + (token.expiresIn * 1000))
        return AuthToken(
            build(userId, expiry),
            expiry,
            token.refreshToken,
            refreshTokenExpiry = Date(currentTimeMillis() + (token.refreshExpiresIn * 1000))
        )
    }

    /** The [userId] is a [UserRepresentation.id]. */
    private fun build(userId: String, expiry: Date): String =
        JWT.create().withExpiresAt(expiry).withSubject(userId).withAudience(audience).withIssuer(issuer).sign(algorithm)
}