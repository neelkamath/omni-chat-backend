package com.neelkamath.omniChatBackend

import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.graphql.routing.*

/** Used to give unique IDs. Increment every usage to get a new one. */
private var userCount = 0

data class VerifiedUser(
    val userId: Int,
    val username: Username,
    val emailAddress: String,
    val firstName: Name,
    val lastName: Name,
    val bio: Bio,
    val password: Password,
) {
    val login: Login = Login(username, password)
    val accessToken: String by buildTokenSet(userId).accessToken

    companion object {
        fun build(account: AccountInput): VerifiedUser = with(account) {
            VerifiedUser(Users.readId(username), username, emailAddress, firstName, lastName, bio, password)
        }
    }
}

/**
 * Returns the [count] of users after creating them, and verifying their emails.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique. The username, password,
 * email, first name, and last name use the format `username<INTEGER>`, `password<INTEGER>`, `<USERNAME>@example.com`
 * `firstName<INTEGER>`, and `lastName<INTEGER>` respectively. The returned [VerifiedUser]s are in ascending order of
 * the `<INTEGER>`.
 */
fun createVerifiedUsers(count: Int): LinkedHashSet<VerifiedUser> = (1..count)
    .map {
        val account = AccountInput(
            Username("username${++userCount}"),
            Password("password$userCount"),
            "username$userCount@example.com",
            Name("firstName$userCount"),
            Name("lastName$userCount"),
        )
        Users.create(account)
        verifyEmailAddress(account.username)
        VerifiedUser.build(account)
    }
    .toLinkedHashSet()
