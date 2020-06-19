package com.neelkamath.omniChat

/** Used to give unique IDs. Increment every usage to get a new one. */
private var userCount = 0

data class VerifiedUser(val info: AccountInfo, val password: String) {
    val login = Login(info.username, password)
}

/**
 * Returns the [count] of users after creating them, and verifying their emails.
 *
 * Regardless of how many times this is called, the user returned is guaranteed to be unique. The username, password,
 * email, first name, and last name use the format `username<INTEGER>`, `password<INTEGER>`, `<USERNAME>@example.com`,
 * `firstName<INTEGER>`, and `lastName<INTEGER>` respectively.
 */
fun createVerifiedUsers(count: Int): List<VerifiedUser> = (0..count).map {
    val account = NewAccount(
        "username${++userCount}",
        "password$userCount",
        "username$userCount@example.com",
        "firstName$userCount",
        "lastName$userCount"
    )
    createUser(account)
    verifyEmailAddress(account.username)
    val userId = findUserByUsername(account.username).id
    with(account) { VerifiedUser(AccountInfo(userId, username, emailAddress, firstName, lastName), password) }
}