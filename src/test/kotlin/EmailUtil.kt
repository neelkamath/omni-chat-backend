package com.neelkamath.omniChat

import com.neelkamath.omniChat.db.tables.Users
import com.neelkamath.omniChat.graphql.routing.Username

/** Sets the [username]'s email address verification status to verified without sending them an email. */
fun verifyEmailAddress(username: Username) {
    val user = Users.read(username)
    Users.verifyEmailAddress(user.emailAddress, user.emailAddressVerificationCode)
}