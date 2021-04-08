package com.neelkamath.omniChatBackend

import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.graphql.routing.Username

/** Sets the [username]'s email address verification status to verified without sending them an email. */
fun verifyEmailAddress(username: Username) {
    val user = Users.read(username)
    Users.verifyEmailAddress(user.emailAddress, user.emailAddressVerificationCode)
}
