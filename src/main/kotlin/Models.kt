package com.neelkamath.omniChat

data class User(
    val username: String,
    val password: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null
)