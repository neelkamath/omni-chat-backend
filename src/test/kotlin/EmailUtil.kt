package com.neelkamath.omniChatBackend

import com.neelkamath.omniChatBackend.db.tables.Users
import com.neelkamath.omniChatBackend.db.tables.readEmailAddress
import com.neelkamath.omniChatBackend.db.tables.readEmailAddressVerificationCode
import com.neelkamath.omniChatBackend.graphql.routing.Username

/** Sets the [username]'s email address verification status to verified without sending them an email. */
fun verifyEmailAddress(username: Username) {
    Users.verifyEmailAddress(Users.readEmailAddress(username), Users.readEmailAddressVerificationCode(username))
}
