package com.neelkamath.omniChat.test.routes

import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.NewUser
import com.neelkamath.omniChat.test.verifyEmail

data class CreatedUser(val login: Login, val userId: String)

/** Creates [count] users, verifies their emails, and returns them. */
fun createVerifiedUsers(count: Int): List<CreatedUser> = (0..count).map {
    val login = Login("username$it", "password")
    createUser(NewUser(login.username, login.password, "username$it@example.com"))
    Auth.verifyEmail(login.username)
    val userId = Auth.findUserByUsername(login.username).id
    CreatedUser(login, userId)
}