package com.neelkamath.omniChat.test.graphql

import com.neelkamath.omniChat.AccountInfo
import com.neelkamath.omniChat.Auth
import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.test.verifyEmail

data class CreatedUser(val info: AccountInfo, val password: String) {
    val login = Login(info.username, password)
}

private var userCount = 0

/**
 * Creates [count] users, verifies their emails, and returns them.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique. The username, password,
 * email, first name, and last name use the format `username<INTEGER>`, `password<INTEGER>`, `<USERNAME>@example.com`,
 * `firstName<INTEGER>`, and `lastName<INTEGER>` respectively.
 */
fun createVerifiedUsers(count: Int): List<CreatedUser> = (0..count).map {
    userCount++
    val login = Login("username$userCount", "password$userCount")
    val account = NewAccount(
        login.username,
        login.password,
        "username$userCount@example.com",
        "firstName$userCount",
        "lastName$userCount"
    )
    createAccount(account)
    Auth.verifyEmail(login.username)
    val userId = Auth.findUserByUsername(login.username).id
    with(account) { CreatedUser(AccountInfo(userId, username, email, firstName, lastName), password) }
}