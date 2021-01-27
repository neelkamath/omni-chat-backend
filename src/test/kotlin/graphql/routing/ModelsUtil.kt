package com.neelkamath.omniChat.graphql.routing

import com.neelkamath.omniChat.db.tables.Users

fun AccountInput.toAccount(): Account =
    Account(Users.read(username).id, username, emailAddress, firstName, lastName, bio)

fun ActionMessageInput.toActionableMessage(): ActionableMessage = ActionableMessage(text, actions)
