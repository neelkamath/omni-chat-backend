package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.test.verifyEmail

data class CreatedUser(val login: Login, val id: String)

private var userCount = 0

/**
 * Creates [count] users, verifies their emails, and returns them.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique.
 */
fun createVerifiedUsers(count: Int): List<CreatedUser> = (0..count).map {
    userCount++
    val login = Login("username$userCount", "password")
    createAccount(NewAccount(login.username, login.password, "username$userCount@example.com"))
    Auth.verifyEmail(login.username)
    val userId = Auth.findUserByUsername(login.username).id
    CreatedUser(login, userId)
}