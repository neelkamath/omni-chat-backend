package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.graphql.routing.*

/** Used to give unique IDs. Increment every usage to get a new one. */
private var userCount = 0

data class VerifiedUser(val info: Account, val password: Password) {
    val login = Login(info.username, password)
    val accessToken = buildTokenSet(info.id).accessToken

    companion object {
        fun build(account: AccountInput): VerifiedUser = with(account) {
            val userId = Users.read(username).id
            VerifiedUser(Account(userId, username, emailAddress, firstName, lastName, bio), password)
        }
    }
}

/**
 * Returns the [count] of users after creating them, and verifying their emails.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique. The username, password,
 * email, first name, and last name use the format `username<INTEGER>`, `password<INTEGER>`, `<USERNAME>@example.com`
 * `firstName<INTEGER>`, and `lastName<INTEGER>` respectively.
 */
fun createVerifiedUsers(count: Int): List<VerifiedUser> = (1..count).map {
    val account = AccountInput(
        Username("username${++userCount}"),
        Password("password$userCount"),
        "username$userCount@example.com",
        Name("firstName$userCount"),
        Name("lastName$userCount")
    )
    Users.create(account)
    verifyEmailAddress(account.username)
    VerifiedUser.build(account)
}
