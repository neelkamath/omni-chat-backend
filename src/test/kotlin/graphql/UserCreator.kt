package com.neelkamath.omniChat.graphql

import com.neelkamath.omniChat.*
import com.neelkamath.omniChat.graphql.operations.mutations.createAccount
import com.neelkamath.omniChat.graphql.operations.queries.requestTokenSet

/** Used to give unique IDs. Increment every usage to get a new one. */
private var userCount = 0

data class SignedInUser(val info: Account, val password: Password) {
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
fun createSignedInUsers(count: Int): List<SignedInUser> = (1..count).map {
    val account = NewAccount(
        Username("username${++userCount}"),
        Password("password$userCount"),
        "username$userCount@example.com",
        Bio(""),
        "firstName$userCount",
        "lastName$userCount"
    )
    createAccount(account)
    verifyEmailAddress(account.username)
    val userId = readUserByUsername(account.username).id
    with(account) { SignedInUser(Account(userId, username, emailAddress, bio, firstName, lastName), password) }
}