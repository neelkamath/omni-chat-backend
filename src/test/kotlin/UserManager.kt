package com.neelkamath.omniChat

/** Used to give unique IDs. Increment every usage to get a new one. */
private var userCount = 0

data class VerifiedUser(val info: Account, val password: Password) {
    val login = Login(info.username, password)
    val accessToken = buildAuthToken(info.id).accessToken

    companion object {
        fun build(account: NewAccount): VerifiedUser = with(account) {
            val userId = readUserByUsername(username).id
            VerifiedUser(Account(userId, username, emailAddress, firstName, lastName, bio), password)
        }
    }
}

/**
 * Returns the [count] of users after creating them, and verifying their emails.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique. The username, password,
 * email, bio, first name, and last name use the format `username<INTEGER>`, `password<INTEGER>`,
 * `<USERNAME>@example.com`, `"bio"`, `firstName<INTEGER>`, and `lastName<INTEGER>` respectively.
 */
fun createVerifiedUsers(count: Int): List<VerifiedUser> = (1..count).map {
    val account = NewAccount(
        Username("username${++userCount}"),
        Password("password$userCount"),
        "username$userCount@example.com",
        "firstName$userCount",
        "lastName$userCount",
        "bio"
    )
    createUser(account)
    verifyEmailAddress(account.username)
    VerifiedUser.build(account)
}