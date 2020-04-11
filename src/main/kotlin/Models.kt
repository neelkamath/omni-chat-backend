package com.neelkamath.omniChat

import java.util.*

data class Login(val username: String, val password: String)

data class InvalidUser(val reason: InvalidUserReason)

enum class InvalidUserReason { NONEXISTENT_USER, INCORRECT_PASSWORD, EMAIL_NOT_VERIFIED, USERNAME_TAKEN }

data class AuthToken(val jwt: String, val expiry: Date, val refreshToken: String, val refreshTokenExpiry: Date)

data class NewUser(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class UserInfo(
    val userId: String,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class UserUpdate(
    val username: String? = null,
    val password: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class UserIdList(val userIdList: Set<String>)

data class UserPublicInfoList(val users: List<UserPublicInfo>)

data class UserPublicInfo(
    val userId: String,
    val username: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
)

data class UserSearchQuery(
    val username: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
) {
    fun hasNoFilters(): Boolean = username == null && firstName == null && lastName == null && email == null
}