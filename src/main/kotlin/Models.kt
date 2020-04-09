package com.neelkamath.omniChat

import java.util.*

data class Login(val username: String? = null, val password: String? = null)

data class User(
    val login: Login? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class InvalidUser(val reason: InvalidUserReason)

enum class InvalidUserReason {
    NONEXISTENT_USER,
    EMAIL_NOT_VERIFIED,
    INCORRECT_PASSWORD,
    USERNAME_TAKEN,
}

data class AuthToken(val jwt: String, val expiry: Date, val refreshToken: String, val refreshTokenExpiry: Date)

data class UserDetails(
    val userId: String,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class Contacts(val userIdList: Set<String>)