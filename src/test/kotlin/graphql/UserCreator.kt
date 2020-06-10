package com.neelkamath.omniChat.test.graphql

import com.neelkamath.omniChat.AccountInfo
import com.neelkamath.omniChat.Login
import com.neelkamath.omniChat.NewAccount
import com.neelkamath.omniChat.findUserByUsername
import com.neelkamath.omniChat.test.graphql.api.mutations.createAccount
import com.neelkamath.omniChat.test.graphql.api.queries.requestTokenSet
import com.neelkamath.omniChat.test.verifyEmailAddress

/** Used to give unique IDs. Increment every usage to get a new one. */
private var userCount = 0

data class SignedInUser(val info: AccountInfo, val password: String) {
    val login = Login(info.username, password)
    val accessToken = requestTokenSet(login).accessToken
}

/**
 * Returns the [count] of users after creating them, and verifying their emails.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique. The username, password,
 * email, first name, and last name use the format `username<INTEGER>`, `password<INTEGER>`, `<USERNAME>@example.com`,
 * `firstName<INTEGER>`, and `lastName<INTEGER>` respectively.
 */
fun createSignedInUsers(count: Int): List<SignedInUser> = (0..count).map {
    val account = NewAccount(
        "username${++userCount}",
        "password$userCount",
        "username$userCount@example.com",
        "firstName$userCount",
        "lastName$userCount"
    )
    createAccount(account)
    verifyEmailAddress(account.username)
    val userId = findUserByUsername(account.username).id
    with(account) { SignedInUser(AccountInfo(userId, username, emailAddress, firstName, lastName), password) }
}